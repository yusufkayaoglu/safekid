package com.safekid.ai.controller;

import com.safekid.ai.dto.*;
import com.safekid.ai.entity.AiAnalysisEntity;
import com.safekid.ai.repository.AiAnalysisRepository;
import com.safekid.ai.service.AiChatService;
import com.safekid.ai.service.AnomalyDetectionService;
import com.safekid.ai.service.DailySummaryService;
import com.safekid.ai.service.RoutePredictionService;
import com.safekid.config.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/parent/ai")
@RequiredArgsConstructor
public class AiController {

    private final RoutePredictionService routePredictionService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final DailySummaryService dailySummaryService;
    private final AiChatService aiChatService;
    private final AiAnalysisRepository aiAnalysisRepository;

    @PostMapping("/route-prediction")
    public ResponseEntity<?> routePrediction(
            @RequestBody RoutePredictionRequest request,
            Authentication auth) {
        try {
            String parentId = SecurityUtils.extractParentId(auth);
            return ResponseEntity.ok(routePredictionService.predict(parentId, request.cocukUniqueId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/anomaly-check")
    public ResponseEntity<?> anomalyCheck(
            @RequestBody RoutePredictionRequest request,
            Authentication auth) {
        try {
            String parentId = SecurityUtils.extractParentId(auth);
            return ResponseEntity.ok(anomalyDetectionService.checkAnomaly(parentId, request.cocukUniqueId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/daily-summary")
    public ResponseEntity<?> dailySummary(
            @RequestBody DailySummaryRequest request,
            Authentication auth) {
        try {
            String parentId = SecurityUtils.extractParentId(auth);
            LocalDate date = request.date() != null ? request.date() : LocalDate.now();
            return ResponseEntity.ok(dailySummaryService.generateSummary(parentId, request.cocukUniqueId(), date));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestBody AiChatRequest request,
            Authentication auth) {
        try {
            String parentId = SecurityUtils.extractParentId(auth);
            return ResponseEntity.ok(aiChatService.chat(parentId, request.cocukUniqueId(), request.message()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AiAlertResponse>> getAlerts(Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);
        List<AiAnalysisEntity> alerts =
                aiAnalysisRepository.findByChild_Parent_EbeveynUniqueIdAndAcknowledgedFalseOrderByCreatedAtDesc(parentId);

        List<AiAlertResponse> response = alerts.stream()
                .map(a -> new AiAlertResponse(
                        a.getId(),
                        a.getChild().getCocukUniqueId(),
                        a.getChild().getCocukAdi(),
                        a.getAnalysisType().name(),
                        a.getResultJson(),
                        a.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable Long alertId,
            Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);
        AiAnalysisEntity alert = aiAnalysisRepository.findById(alertId)
                .filter(a -> a.getChild().getParent().getEbeveynUniqueId().equals(parentId))
                .orElse(null);

        if (alert == null) {
            return ResponseEntity.notFound().build();
        }

        alert.setAcknowledged(true);
        aiAnalysisRepository.save(alert);
        return ResponseEntity.ok().build();
    }
}
