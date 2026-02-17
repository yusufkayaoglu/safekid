package com.safekid.child.dto;

import java.time.Instant;

public record MapChildLocation(
        String childId,
        String childName,
        Double lat,
        Double lng,
        Instant lastSeenAt,
        boolean online
) {}
