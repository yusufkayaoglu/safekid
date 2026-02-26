package com.safekid.geofence.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Çocuk güvenli alanın dışına çıktığında SSE üzerinden ebeveyene gönderilen olay.
 *
 * <p>SSE event name: {@code geofence-breach}
 *
 * <p>Frontend bu event'i dinleyerek kullanıcıya uygulama içi uyarı göstermelidir.
 */
public record GeofenceAlertEvent(

        @Schema(description = "Sabit değer: GEOFENCE_BREACH")
        String type,

        @Schema(description = "Çocuğun benzersiz ID'si")
        String cocukId,

        @Schema(description = "Çocuğun tam adı")
        String cocukAdi,

        @Schema(description = "Çocuğun o anki enlemi")
        double lat,

        @Schema(description = "Çocuğun o anki boylamı")
        double lng,

        @Schema(description = "İhlal edilen güvenli alanın ID'si")
        Long geofenceId,

        @Schema(description = "İhlal edilen güvenli alanın adı")
        String alanAdi,

        @Schema(description = "İhlalin gerçekleştiği zaman (UTC)")
        Instant zaman
) {}
