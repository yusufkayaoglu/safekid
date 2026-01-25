package com.safekid.auth.dto;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        Instant expiresAt,
        String ebeveynUniqueId,
        String ebeveynAdi,
        String ebeveynSoyadi
) {}
