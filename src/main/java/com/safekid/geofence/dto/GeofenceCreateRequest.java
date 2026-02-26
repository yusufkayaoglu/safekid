package com.safekid.geofence.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Ebeveyn tarafından gönderilen güvenli alan (polygon) oluşturma isteği.
 *
 * <p>Koordinatlar GeoJSON standardında <b>[longitude, latitude]</b> çifti
 * olarak gönderilmelidir. Frontend'de Google Maps polygon'ından alınan
 * LatLng değerleri sıralamaya dikkat edilerek dönüştürülmelidir.
 *
 * <p>Örnek body:
 * <pre>
 * {
 *   "cocukId": "01HXYZ...",
 *   "alanAdi": "Okul Bölgesi",
 *   "koordinatlar": [
 *     [28.97953, 41.01384],
 *     [28.98120, 41.01384],
 *     [28.98120, 41.01520],
 *     [28.97953, 41.01520],
 *     [28.97953, 41.01384]
 *   ]
 * }
 * </pre>
 * İlk ve son nokta aynı olmalıdır (kapalı halka). Farklıysa backend otomatik kapatır.
 */
public record GeofenceCreateRequest(

        @Schema(description = "Güvenli alanın atanacağı çocuğun benzersiz ID'si", example = "01HXYZ...")
        String cocukId,

        @Schema(description = "Alan için açıklayıcı isim", example = "Okul Bölgesi")
        String alanAdi,

        @Schema(
                description = "Polygon köşe noktaları. Her eleman [longitude, latitude] çiftidir. " +
                              "En az 3 nokta olmalı; ilk ve son nokta aynıysa kapalı halka varsayılır.",
                example = "[[28.97953,41.01384],[28.98120,41.01384],[28.98120,41.01520],[28.97953,41.01520]]"
        )
        List<List<Double>> koordinatlar
) {}
