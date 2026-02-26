package com.safekid.geofence.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Oluşturulan / listelenen güvenli alan yanıtı.
 * Koordinatlar GeoJSON standardında [longitude, latitude] sırasıyla döner.
 */
public record GeofenceResponse(

        @Schema(description = "Güvenli alan ID'si")
        Long id,

        @Schema(description = "Alan sahibi çocuğun ID'si")
        String cocukId,

        @Schema(description = "Alan adı")
        String alanAdi,

        @Schema(description = "Polygon koordinatları — her eleman [lng, lat]")
        List<List<Double>> koordinatlar,

        @Schema(description = "Alan aktif mi?")
        boolean aktif,

        @Schema(description = "Oluşturulma zamanı (UTC)")
        Instant olusturmaTarihi
) {}
