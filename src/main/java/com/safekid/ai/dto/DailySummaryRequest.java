package com.safekid.ai.dto;

import java.time.LocalDate;

public record DailySummaryRequest(
        String cocukUniqueId,
        LocalDate date
) {}
