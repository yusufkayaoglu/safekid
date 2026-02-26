package com.safekid.geofence.controller;

import com.safekid.billing.service.SubscriptionService;
import com.safekid.config.SecurityUtils;
import com.safekid.geofence.dto.GeofenceAlertResponse;
import com.safekid.geofence.dto.GeofenceCreateRequest;
import com.safekid.geofence.dto.GeofenceResponse;
import com.safekid.geofence.service.GeofenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Ebeveyne ait güvenli alan (polygon geofence) yönetimi.
 *
 * <p>Tüm endpoint'ler ROLE_PARENT yetkisi gerektirir (JWT Bearer token).
 *
 * <p>SSE Entegrasyonu: Çocuğun konumu güncellendiğinde backend otomatik olarak
 * tüm aktif geofence'leri kontrol eder. İhlal varsa ebeveynin SSE kanalına
 * {@code geofence-breach} olayı gönderilir.
 *
 * <pre>
 * EventSource dinleme örneği (JavaScript):
 *
 *   const es = new EventSource('/parent/children/live', { headers: { Authorization: 'Bearer ...' } });
 *
 *   es.addEventListener('geofence-breach', (e) => {
 *     const alert = JSON.parse(e.data);
 *     console.log(`${alert.cocukAdi} güvenli alandan çıktı: ${alert.alanAdi}`);
 *   });
 * </pre>
 */
@RestController
@RequestMapping("/parent/geofence")
@Tag(name = "Geofence", description = "Polygon tabanlı güvenli alan (jeo-çit) yönetimi")
@SecurityRequirement(name = "Bearer Authentication")
public class GeofenceController {

    private final GeofenceService geofenceService;
    private final SubscriptionService subscriptionService;

    public GeofenceController(GeofenceService geofenceService, SubscriptionService subscriptionService) {
        this.geofenceService = geofenceService;
        this.subscriptionService = subscriptionService;
    }

    private void requirePremium(String parentId) {
        if (!subscriptionService.isPremium(parentId)) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Bu özellik premium üyelik gerektirir");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  POST /parent/geofence  →  Yeni alan oluştur
    // ──────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Yeni güvenli alan oluştur",
            description = """
                    Ebeveyn Google Maps üzerinde çizdiği polygon koordinatlarını gönderir.
                    Koordinatlar **[longitude, latitude]** sırasında olmalıdır (GeoJSON standardı).
                    İlk ve son nokta aynı değilse sistem otomatik kapatır.
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Alan oluşturuldu",
                            content = @Content(schema = @Schema(implementation = GeofenceResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Geçersiz koordinatlar"),
                    @ApiResponse(responseCode = "403", description = "Çocuk bu ebeveyine ait değil")
            }
    )
    public GeofenceResponse create(Authentication auth,
                                   @RequestBody GeofenceCreateRequest req) {
        String parentId = SecurityUtils.extractParentId(auth);
        requirePremium(parentId);
        return geofenceService.create(parentId, req);
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /parent/geofence/{childId}  →  Çocuğun aktif alanları
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/{childId}")
    @Operation(
            summary = "Çocuğun aktif güvenli alanlarını listele",
            description = "Belirtilen çocuğa ait tüm aktif polygon geofence'leri döner."
    )
    public List<GeofenceResponse> list(Authentication auth,
                                       @PathVariable String childId) {
        String parentId = SecurityUtils.extractParentId(auth);
        requirePremium(parentId);
        return geofenceService.listByChild(parentId, childId);
    }

    // ──────────────────────────────────────────────────────────────
    //  PUT /parent/geofence/{geofenceId}  →  Alan güncelle
    // ──────────────────────────────────────────────────────────────

    @PutMapping("/{geofenceId}")
    @Operation(
            summary = "Güvenli alanı güncelle",
            description = "Alan adını ve/veya koordinatlarını günceller. Gönderilmeyen alanlar değişmez."
    )
    public GeofenceResponse update(Authentication auth,
                                   @PathVariable Long geofenceId,
                                   @RequestBody GeofenceCreateRequest req) {
        String parentId = SecurityUtils.extractParentId(auth);
        requirePremium(parentId);
        return geofenceService.update(parentId, geofenceId, req);
    }

    // ──────────────────────────────────────────────────────────────
    //  DELETE /parent/geofence/{geofenceId}  →  Alan sil (pasife al)
    // ──────────────────────────────────────────────────────────────

    @DeleteMapping("/{geofenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Güvenli alanı sil",
            description = "Güvenli alanı soft-delete ile pasife alır. " +
                          "Silinen alan bir daha kontrol edilmez."
    )
    public void delete(Authentication auth,
                       @PathVariable Long geofenceId) {
        String parentId = SecurityUtils.extractParentId(auth);
        requirePremium(parentId);
        geofenceService.delete(parentId, geofenceId);
    }

    // ──────────────────────────────────────────────────────────────
    //  ALERT ENDPOİNT'LERİ
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/alerts")
    @Operation(
            summary = "İhlal bildirimlerini listele",
            description = """
                    Ebeveyine ait tüm güvenli alan ihlal bildirimlerini döner, en yeniden eskiye.
                    `sadeceokunmamis=true` ile yalnızca okunmamış bildirimler filtrelenebilir.
                    SSE bağlantısı yokken kaçırılan alertleri bu endpoint ile çekin.
                    """
    )
    public List<GeofenceAlertResponse> listAlerts(
            Authentication auth,
            @RequestParam(name = "sadeceokunmamis", defaultValue = "false") boolean sadeceokunmamis) {
        String parentId = SecurityUtils.extractParentId(auth);
        requirePremium(parentId);
        return geofenceService.listAlerts(parentId, sadeceokunmamis);
    }

    @GetMapping("/alerts/unread-count")
    @Operation(summary = "Okunmamış bildirim sayısı", description = "Uygulama rozeti için kullanılabilir.")
    public java.util.Map<String, Long> unreadCount(Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);
        requirePremium(parentId);
        long count = geofenceService.unreadCount(parentId);
        return java.util.Map.of("okunmamis", count);
    }

    @PutMapping("/alerts/{alertId}/read")
    @Operation(summary = "Bildirimi okundu yap")
    public void markRead(Authentication auth,
                         @PathVariable Long alertId) {
        String parentId = SecurityUtils.extractParentId(auth);
        requirePremium(parentId);
        geofenceService.markRead(parentId, alertId);
    }

    @PutMapping("/alerts/read-all")
    @Operation(summary = "Tüm bildirimleri okundu yap")
    public void markAllRead(Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);
        requirePremium(parentId);
        geofenceService.markAllRead(parentId);
    }
}
