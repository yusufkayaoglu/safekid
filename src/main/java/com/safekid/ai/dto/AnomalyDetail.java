package com.safekid.ai.dto;

public record AnomalyDetail(
        String type,
        String description,
        String severity,
        Double lat,
        Double lng
) {}
