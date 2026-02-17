package com.safekid.ai.dto;

import java.util.List;

public record AnomalyDetectionResponse(
        String cocukUniqueId,
        boolean anomalyDetected,
        List<AnomalyDetail> anomalies,
        String summary
) {}
