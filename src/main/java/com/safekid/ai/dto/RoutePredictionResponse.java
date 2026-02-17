package com.safekid.ai.dto;

public record RoutePredictionResponse(
        String cocukUniqueId,
        String predictedDestination,
        Double predictedLat,
        Double predictedLng,
        Double confidencePercent,
        String reasoning
) {}
