package com.safekid.geofence.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Güvenli alan ihlal bildirimi")
public record GeofenceAlertResponse(

        @Schema(description = "Bildirim ID'si")
        Long id,

        @Schema(description = "Çocuğun ID'si")
        String cocukId,

        @Schema(description = "Çocuğun tam adı")
        String cocukAdi,

        @Schema(description = "İhlal edilen alanın ID'si")
        Long geofenceId,

        @Schema(description = "İhlal edilen alanın adı")
        String alanAdi,

        @Schema(description = "İhlal anındaki enlem")
        double lat,

        @Schema(description = "İhlal anındaki boylam")
        double lng,

        @Schema(description = "İhlal zamanı (UTC)")
        Instant zaman,

        @Schema(description = "Ebeveyn tarafından okundu mu?")
        boolean okundu
) {}
