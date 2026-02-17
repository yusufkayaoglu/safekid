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
    private final ObjectMapper objectMapper;

    @Value("${safekid.ai.speed-threshold-kmh}")
    private double speedThresholdKmh;

    private static final String SYSTEM_PROMPT = """
            Sen bir çocuk güvenlik takip sisteminin anomali tespit modülüsün.
            Verilen konum verilerini analiz ederek olağandışı durumları tespit et.

            Kontrol et:
            - Anormal hız (çocuk için olağandışı yüksek hız)
            - Gece saatlerinde (23:00-06:00) hareket
            - Beklenmedik bölgeler
            - Ani yön değişiklikleri

            SADECE aşağıdaki JSON formatında yanıt ver:
            {
              "anomalyDetected": true/false,
              "anomalies": [
                {
                  "type": "HIGH_SPEED | NIGHT_MOVEMENT | UNKNOWN_AREA | SUDDEN_DIRECTION_CHANGE",
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

        for (int i = 1; i < recentLocations.size(); i++) {
            double speed = dataCollector.calculateSpeedKmh(recentLocations.get(i - 1), recentLocations.get(i));
            if (speed > speedThresholdKmh) {
                localFlags.add(String.format("Yüksek hız tespit: %.1f km/h", speed));
                suspiciousFound = true;
            }
        }

        // Check night movement
        ZonedDateTime lastTime = recentLocations.getLast().getRecordedAt()
                .atZone(ZoneId.of("Europe/Istanbul"));
        int hour = lastTime.getHour();
        if (hour >= 23 || hour < 6) {
            localFlags.add("Gece saatinde hareket tespit edildi: " + lastTime.toLocalTime());
            suspiciousFound = true;
        }

        if (!suspiciousFound) {
            return new AnomalyDetectionResponse(cocukUniqueId, false, List.of(), "Anomali tespit edilmedi.");
        }

        // Call Claude for detailed analysis
        String locationData = dataCollector.formatLocationsForPrompt(recentLocations);
        String userMessage = String.format("""
                Çocuk: %s %s
                Yerel ön-filtreleme bulguları: %s

                === Son Konum Verileri ===
                %s

                Bu verileri analiz et ve anomalileri belirle.
                """,
                child.getCocukAdi(), child.getCocukSoyadi(),
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

        for (int i = 1; i < locations.size(); i++) {
            double speed = dataCollector.calculateSpeedKmh(locations.get(i - 1), locations.get(i));
            if (speed > speedThresholdKmh) {
                localFlags.add(String.format("Yüksek hız: %.1f km/h", speed));
                suspiciousFound = true;
            }
        }

        ZonedDateTime lastTime = locations.getLast().getRecordedAt().atZone(ZoneId.of("Europe/Istanbul"));
        int hour = lastTime.getHour();
        if (hour >= 23 || hour < 6) {
            localFlags.add("Gece hareketi: " + lastTime.toLocalTime());
            suspiciousFound = true;
        }

        if (!suspiciousFound) return;

        String locationData = dataCollector.formatLocationsForPrompt(locations);
        String userMessage = String.format("""
                Çocuk: %s %s
                Yerel bulgular: %s

                === Son Konum Verileri ===
                %s

                Analiz et ve anomalileri belirle.
                """,
                child.getCocukAdi(), child.getCocukSoyadi(),
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
