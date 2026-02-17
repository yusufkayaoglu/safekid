package com.safekid.ai.dto;

public record DailySummaryResponse(
        String cocukUniqueId,
        String date,
        String summary,
        int totalLocationPoints,
        double totalDistanceKm
) {}
