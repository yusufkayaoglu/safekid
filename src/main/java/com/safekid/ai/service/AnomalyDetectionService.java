package com.safekid.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safekid.ai.client.ClaudeApiClient;
import com.safekid.ai.dto.AnomalyDetail;
import com.safekid.ai.dto.AnomalyDetectionResponse;
import com.safekid.ai.entity.AiAnalysisEntity;
import com.safekid.ai.entity.AnalysisType;
import com.safekid.ai.repository.AiAnalysisRepository;
import com.safekid.child.entity.CocukKonumEntity;
import com.safekid.child.repository.CocukKonumRepository;
import com.safekid.child.sse.SseEmitterRegistry;
import com.safekid.notification.FcmService;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final ClaudeApiClient claudeApiClient;
    private final CocukKonumRepository konumRepository;
    private final ChildRepository childRepository;
    private final AiAnalysisRepository analysisRepository;
    private final LocationDataCollector dataCollector;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;

    @Value("${safekid.ai.speed-threshold-kmh}")
    private double speedThresholdKmh;

    private static final String SYSTEM_PROMPT = """
            Sen bir çocuk güvenlik takip sisteminin anomali tespit modülüsün.
            Verilen konum verilerini analiz ederek GERÇEK olağandışı durumları tespit et.

            ÖNEMLİ — GPS DOĞRULUĞU HAKKINDA:
            - Mobil GPS doğruluğu tipik olarak 5-50 metre arasındadır.
            - 100 metreden küçük konum sıçramaları GPS gürültüsüdür, gerçek hareket DEĞİLDİR.
            - Çocuk sabit duruyorsa bile GPS konumu sürekli hafifçe değişir, bu normaldir.
            - Sabit konumda 8-15 km/h gibi anlık hız değerleri GPS hatasından kaynaklanır, alarm üretme.

            SADECE şunları flag'le:
            - HIGH_SPEED: 60 saniyelik pencerede ortalama hız > eşik değeri (gerçek araç hareketi)
            - NIGHT_MOVEMENT: Gece 23:00-06:00 arasında VE çocuk gerçekten yer değiştirmiş (>150m)
            - UNKNOWN_AREA: Tamamen bilinmeyen bir bölgeye ciddi mesafe kat edilmişse

            FLAGLEME:
            - SUDDEN_DIRECTION_CHANGE: GPS gürültüsü olduğu için bu tipi KULLANMA
            - Sabit konumda olan çocuk için asla NIGHT_MOVEMENT flagleme
            - 100m altındaki konum sıçramalarını anomali sayma

            SADECE aşağıdaki JSON formatında yanıt ver:
            {
              "anomalyDetected": true/false,
              "anomalies": [
                {
                  "type": "HIGH_SPEED | NIGHT_MOVEMENT | UNKNOWN_AREA",
                  "description": "Açıklama",
                  "severity": "LOW | MEDIUM | HIGH | CRITICAL",
                  "lat": 0.0,
                  "lng": 0.0
                }
              ],
              "summary": "Genel değerlendirme"
            }
            """;

    public AnomalyDetectionResponse checkAnomaly(String parentId, String cocukUniqueId) {
        ChildEntity child = childRepository
                .findByCocukUniqueIdAndParent_EbeveynUniqueId(cocukUniqueId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Çocuk bulunamadı"));

        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        List<CocukKonumEntity> recentLocations =
                konumRepository.findByChild_CocukUniqueIdAndRecordedAtAfterOrderByRecordedAtAsc(
                        cocukUniqueId, oneHourAgo);

        if (recentLocations.size() < 2) {
            return new AnomalyDetectionResponse(cocukUniqueId, false, List.of(), "Yeterli veri yok.");
        }

        // Local pre-filtering
        List<String> localFlags = new ArrayList<>();
        boolean suspiciousFound = false;

        // GPS gürültüsü eşiği: 50m — evde uyuyan çocuğun GPS sapması tipik olarak <20m
        boolean stationary = dataCollector.isStationary(recentLocations, 50);

        // Anlık hız yerine 60 saniyelik kayan pencere ortalaması kullan (GPS gürültüsüne karşı)
        double maxWindowSpeed = 0;
        for (int i = 1; i < recentLocations.size(); i++) {
            double windowSpeed = dataCollector.calculateWindowAverageSpeedKmh(recentLocations, i, 60);
            maxWindowSpeed = Math.max(maxWindowSpeed, windowSpeed);
        }
        if (maxWindowSpeed > speedThresholdKmh) {
            localFlags.add(String.format("Yüksek ortalama hız tespit: %.1f km/h (60 sn pencere)", maxWindowSpeed));
            suspiciousFound = true;
        }

        // Gece hareketi: sadece çocuk gerçekten yer değiştirmişse flag'le
        ZonedDateTime lastTime = recentLocations.getLast().getRecordedAt()
                .atZone(ZoneId.of("Europe/Istanbul"));
        int hour = lastTime.getHour();
        if ((hour >= 23 || hour < 6) && !stationary) {
            localFlags.add("Gece saatinde hareket tespit edildi: " + lastTime.toLocalTime());
            suspiciousFound = true;
        }

        if (!suspiciousFound) {
            return new AnomalyDetectionResponse(cocukUniqueId, false, List.of(), "Anomali tespit edilmedi.");
        }

        // Call Claude for detailed analysis
        String locationData = dataCollector.formatLocationsForPrompt(recentLocations);
        String stationaryNote = stationary
                ? "NOT: Çocuk sabit konumda (GPS sürüklenmesi var, gerçek hareket yok).\n"
                : "";
        String userMessage = String.format("""
                Çocuk: %s %s
                %sYerel ön-filtreleme bulguları: %s

                === Son Konum Verileri ===
                %s

                Bu verileri analiz et ve anomalileri belirle.
                """,
                child.getCocukAdi(), child.getCocukSoyadi(),
                stationaryNote,
                String.join("; ", localFlags),
                locationData);

        String aiResponse = claudeApiClient.sendMessage(SYSTEM_PROMPT, userMessage);
        AnomalyDetectionResponse result = parseAnomalyResponse(cocukUniqueId, aiResponse);

        if (result.anomalyDetected()) {
            // Save as unacknowledged alert
            AiAnalysisEntity analysis = new AiAnalysisEntity();
            analysis.setChild(child);
            analysis.setAnalysisType(AnalysisType.ANOMALY_DETECTION);
            analysis.setResultJson(aiResponse);
            analysis.setAcknowledged(false);
            analysisRepository.save(analysis);

            // SSE notification
            String parentUniqueId = child.getParent().getEbeveynUniqueId();
            sseEmitterRegistry.sendEvent(parentUniqueId, "ai-anomaly-alert", Map.of(
                    "cocukUniqueId", cocukUniqueId,
                    "cocukAdi", child.getCocukAdi(),
                    "summary", result.summary(),
                    "alertId", analysis.getId()
            ));

            // FCM push notification
            fcmService.sendPush(
                    child.getParent().getFcmToken(),
                    "⚠️ Anomali: " + child.getCocukAdi(),
                    result.summary()
            );
        }

        return result;
    }

    @Scheduled(fixedDelayString = "${safekid.ai.anomaly-check-interval-ms}")
    public void scheduledAnomalyCheck() {
        log.info("Running scheduled anomaly check for all children...");
        List<ChildEntity> allChildren = childRepository.findAll();
        for (ChildEntity child : allChildren) {
            try {
                checkAnomalyInternal(child);
            } catch (Exception e) {
                log.error("Anomaly check failed for child {}: {}", child.getCocukUniqueId(), e.getMessage());
            }
        }
    }

    private void checkAnomalyInternal(ChildEntity child) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        List<CocukKonumEntity> locations =
                konumRepository.findByChild_CocukUniqueIdAndRecordedAtAfterOrderByRecordedAtAsc(
                        child.getCocukUniqueId(), oneHourAgo);

        if (locations.size() < 2) return;

        boolean suspiciousFound = false;
        List<String> localFlags = new ArrayList<>();

        boolean stationary = dataCollector.isStationary(locations, 50);

        double maxWindowSpeed = 0;
        for (int i = 1; i < locations.size(); i++) {
            double windowSpeed = dataCollector.calculateWindowAverageSpeedKmh(locations, i, 60);
            maxWindowSpeed = Math.max(maxWindowSpeed, windowSpeed);
        }
        if (maxWindowSpeed > speedThresholdKmh) {
            localFlags.add(String.format("Yüksek ortalama hız: %.1f km/h (60 sn pencere)", maxWindowSpeed));
            suspiciousFound = true;
        }

        ZonedDateTime lastTime = locations.getLast().getRecordedAt().atZone(ZoneId.of("Europe/Istanbul"));
        int hour = lastTime.getHour();
        if ((hour >= 23 || hour < 6) && !stationary) {
            localFlags.add("Gece hareketi: " + lastTime.toLocalTime());
            suspiciousFound = true;
        }

        if (!suspiciousFound) return;

        String locationData = dataCollector.formatLocationsForPrompt(locations);
        String stationaryNote = stationary
                ? "NOT: Çocuk sabit konumda (GPS sürüklenmesi var, gerçek hareket yok).\n"
                : "";
        String userMessage = String.format("""
                Çocuk: %s %s
                %sYerel bulgular: %s

                === Son Konum Verileri ===
                %s

                Analiz et ve anomalileri belirle.
                """,
                child.getCocukAdi(), child.getCocukSoyadi(),
                stationaryNote,
                String.join("; ", localFlags), locationData);

        String aiResponse = claudeApiClient.sendMessage(SYSTEM_PROMPT, userMessage);
        AnomalyDetectionResponse result = parseAnomalyResponse(child.getCocukUniqueId(), aiResponse);

        if (result.anomalyDetected()) {
            AiAnalysisEntity analysis = new AiAnalysisEntity();
            analysis.setChild(child);
            analysis.setAnalysisType(AnalysisType.ANOMALY_DETECTION);
            analysis.setResultJson(aiResponse);
            analysis.setAcknowledged(false);
            analysisRepository.save(analysis);

            String parentId = child.getParent().getEbeveynUniqueId();
            sseEmitterRegistry.sendEvent(parentId, "ai-anomaly-alert", Map.of(
                    "cocukUniqueId", child.getCocukUniqueId(),
                    "cocukAdi", child.getCocukAdi(),
                    "summary", result.summary(),
                    "alertId", analysis.getId()
            ));

            // FCM push notification
            fcmService.sendPush(
                    child.getParent().getFcmToken(),
                    "⚠️ Anomali: " + child.getCocukAdi(),
                    result.summary()
            );
        }
    }

    private AnomalyDetectionResponse parseAnomalyResponse(String cocukUniqueId, String json) {
        try {
            String cleaned = json.trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            JsonNode root = objectMapper.readTree(cleaned);
            boolean detected = root.path("anomalyDetected").asBoolean(false);
            String summary = root.path("summary").asText("");

            List<AnomalyDetail> anomalies = new ArrayList<>();
            JsonNode arr = root.path("anomalies");
            if (arr.isArray()) {
                for (JsonNode item : arr) {
                    anomalies.add(new AnomalyDetail(
                            item.path("type").asText(),
                            item.path("description").asText(),
                            item.path("severity").asText(),
                            item.path("lat").asDouble(0),
                            item.path("lng").asDouble(0)
                    ));
                }
            }
            return new AnomalyDetectionResponse(cocukUniqueId, detected, anomalies, summary);
        } catch (Exception e) {
            log.warn("Failed to parse anomaly JSON: {}", json);
            return new AnomalyDetectionResponse(cocukUniqueId, false, List.of(), json);
        }
    }
}
