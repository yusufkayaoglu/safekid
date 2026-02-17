package com.safekid.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safekid.ai.client.ClaudeApiClient;
import com.safekid.ai.dto.RoutePredictionResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutePredictionService {

    private final ClaudeApiClient claudeApiClient;
    private final CocukKonumRepository konumRepository;
    private final ChildRepository childRepository;
    private final AiAnalysisRepository analysisRepository;
    private final LocationDataCollector dataCollector;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Sen bir çocuk güvenlik takip sisteminin rota tahmin modülüsün.
            Verilen konum geçmişine dayanarak çocuğun nereye gittiğini tahmin et.

            SADECE aşağıdaki JSON formatında yanıt ver, başka hiçbir metin ekleme:
            {
              "predictedDestination": "Tahmin edilen yer adı",
              "predictedLat": 0.0,
              "predictedLng": 0.0,
              "confidencePercent": 0.0,
              "reasoning": "Tahmin gerekçesi"
            }
            """;

    public RoutePredictionResponse predict(String parentId, String cocukUniqueId) {
        ChildEntity child = childRepository
                .findByCocukUniqueIdAndParent_EbeveynUniqueId(cocukUniqueId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Çocuk bulunamadı"));

        // Son 100 konum
        List<CocukKonumEntity> recentLocations =
                konumRepository.findTop100ByChild_CocukUniqueIdOrderByRecordedAtDesc(cocukUniqueId);

        if (recentLocations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Konum verisi bulunamadı");
        }

        // Geçen haftanın aynı günü/saati verileri
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"));
        DayOfWeek today = now.getDayOfWeek();
        Instant weekAgoStart = now.minusDays(8).toInstant();
        Instant weekAgoEnd = now.minusDays(6).toInstant();
        List<CocukKonumEntity> historicalLocations =
                konumRepository.findByChild_CocukUniqueIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                        cocukUniqueId, weekAgoStart, weekAgoEnd);

        String recentData = dataCollector.formatLocationsForPrompt(recentLocations.reversed());
        String historicalData = historicalLocations.isEmpty()
                ? "Geçmiş hafta verisi yok."
                : dataCollector.formatLocationsForPrompt(historicalLocations);

        String userMessage = String.format("""
                Çocuk: %s %s
                Bugün: %s, Saat: %s

                === Son Konum Verileri ===
                %s

                === Geçen Hafta Aynı Gün Verileri ===
                %s

                Bu verilere dayanarak çocuğun şu an nereye gittiğini tahmin et.
                """,
                child.getCocukAdi(), child.getCocukSoyadi(),
                today, now.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
                recentData, historicalData);

        String aiResponse = claudeApiClient.sendMessage(SYSTEM_PROMPT, userMessage);

        // Save analysis
        AiAnalysisEntity analysis = new AiAnalysisEntity();
        analysis.setChild(child);
        analysis.setAnalysisType(AnalysisType.ROUTE_PREDICTION);
        analysis.setResultJson(aiResponse);
        analysis.setAcknowledged(true);
        analysisRepository.save(analysis);

        return parseRoutePrediction(cocukUniqueId, aiResponse);
    }

    private RoutePredictionResponse parseRoutePrediction(String cocukUniqueId, String json) {
        try {
            // Extract JSON from response (in case Claude wraps it)
            String cleaned = json.trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            JsonNode node = objectMapper.readTree(cleaned);
            return new RoutePredictionResponse(
                    cocukUniqueId,
                    node.path("predictedDestination").asText("Bilinmiyor"),
                    node.path("predictedLat").asDouble(0),
                    node.path("predictedLng").asDouble(0),
                    node.path("confidencePercent").asDouble(0),
                    node.path("reasoning").asText("")
            );
        } catch (Exception e) {
            log.warn("Failed to parse route prediction JSON, returning raw: {}", json);
            return new RoutePredictionResponse(cocukUniqueId, "Bilinmiyor", 0.0, 0.0, 0.0, json);
        }
    }
}
