package com.safekid.child.dto;

import java.time.Instant;

public record LocationResponse(
        String childId,
        Double lat,
        Double lng,
        Instant recordedAt
) {}
