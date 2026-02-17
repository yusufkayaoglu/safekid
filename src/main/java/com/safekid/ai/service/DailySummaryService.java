package com.safekid.ai.service;

import com.safekid.ai.client.ClaudeApiClient;
import com.safekid.ai.dto.DailySummaryResponse;
import com.safekid.ai.entity.AiAnalysisEntity;
import com.safekid.ai.entity.AnalysisType;
import com.safekid.ai.repository.AiAnalysisRepository;
import com.safekid.child.entity.CocukKonumEntity;
import com.safekid.child.repository.CocukKonumRepository;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final ClaudeApiClient claudeApiClient;
    private final CocukKonumRepository konumRepository;
    private final ChildRepository childRepository;
    private final AiAnalysisRepository analysisRepository;
    private final LocationDataCollector dataCollector;

    private static final ZoneId TZ = ZoneId.of("Europe/Istanbul");

    private static final String SYSTEM_PROMPT = """
            Sen bir çocuk güvenlik takip sisteminin günlük özet modülüsün.
            Verilen konum geçmişine dayanarak çocuğun gün içindeki hareketlerinin
            Türkçe anlatısal bir özetini üret.

            Özetin şunları içermeli:
            - Sabah ilk hareket saati
            - Gün içinde ziyaret edilen muhtemel yerler
            - En çok vakit geçirilen bölge
            - Toplam tahmini mesafe
            - Dikkat çeken durumlar (varsa)

            Doğal, anlaşılır bir Türkçe ile yaz. Ebeveyne hitap et.
            Sadece özet metnini yaz, JSON formatı kullanma.
            """;

    public DailySummaryResponse generateSummary(String parentId, String cocukUniqueId, LocalDate date) {
        ChildEntity child = childRepository
                .findByCocukUniqueIdAndParent_EbeveynUniqueId(cocukUniqueId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Çocuk bulunamadı"));

        ZonedDateTime dayStart = date.atStartOfDay(TZ);
        ZonedDateTime dayEnd = dayStart.plusDays(1);

        List<CocukKonumEntity> dayLocations =
                konumRepository.findByChild_CocukUniqueIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                        cocukUniqueId, dayStart.toInstant(), dayEnd.toInstant());

        if (dayLocations.isEmpty()) {
            return new DailySummaryResponse(cocukUniqueId, date.toString(),
                    "Bu tarihte konum verisi bulunamadı.", 0, 0.0);
        }

        String locationData = dataCollector.formatLocationsForPrompt(dayLocations);
        double totalDistanceKm = dataCollector.calculateTotalDistanceKm(dayLocations);

        String userMessage = String.format("""
                Çocuk: %s %s
                Tarih: %s
                Toplam konum noktası: %d
                Toplam mesafe: %.2f km

                === Konum Verileri ===
                %s

                Bu verilere dayanarak günlük özet üret.
                """,
                child.getCocukAdi(), child.getCocukSoyadi(),
                date, dayLocations.size(), totalDistanceKm, locationData);

        String aiResponse = claudeApiClient.sendMessage(SYSTEM_PROMPT, userMessage);

        // Save analysis
        AiAnalysisEntity analysis = new AiAnalysisEntity();
        analysis.setChild(child);
        analysis.setAnalysisType(AnalysisType.DAILY_SUMMARY);
        analysis.setResultJson(aiResponse);
        analysis.setAcknowledged(true);
        analysisRepository.save(analysis);

        return new DailySummaryResponse(
                cocukUniqueId, date.toString(), aiResponse,
                dayLocations.size(), Math.round(totalDistanceKm * 100.0) / 100.0);
    }

    @Scheduled(cron = "${safekid.ai.daily-summary-cron}", zone = "Europe/Istanbul")
    public void scheduledDailySummary() {
        log.info("Running scheduled daily summary for all children...");
        LocalDate today = LocalDate.now(TZ);
        List<ChildEntity> allChildren = childRepository.findAll();
        for (ChildEntity child : allChildren) {
            try {
                String parentId = child.getParent().getEbeveynUniqueId();
                generateSummary(parentId, child.getCocukUniqueId(), today);
                log.info("Daily summary generated for child {}", child.getCocukUniqueId());
            } catch (Exception e) {
                log.error("Daily summary failed for child {}: {}", child.getCocukUniqueId(), e.getMessage());
            }
        }
    }
}
