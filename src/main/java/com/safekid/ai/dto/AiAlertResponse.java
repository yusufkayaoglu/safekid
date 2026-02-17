package com.safekid.ai.dto;

import java.time.Instant;

public record AiAlertResponse(
        Long alertId,
        String cocukUniqueId,
        String cocukAdi,
        String analysisType,
        String resultJson,
        Instant createdAt
) {}
