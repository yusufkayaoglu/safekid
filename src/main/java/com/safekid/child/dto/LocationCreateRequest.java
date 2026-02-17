package com.safekid.child.dto;

import java.time.Instant;

public record LocationCreateRequest(
        Double lat,
        Double lng,
        Instant recordedAt
) {}
