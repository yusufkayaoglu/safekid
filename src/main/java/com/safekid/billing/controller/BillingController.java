package com.safekid.billing.controller;

import com.safekid.billing.dto.PurchaseVerifyRequest;
import com.safekid.billing.dto.SubscriptionStatusResponse;
import com.safekid.billing.service.SubscriptionService;
import com.safekid.config.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Google Play abonelik yönetimi endpoint'leri.
 *
 * <p>Tüm endpoint'ler {@code ROLE_PARENT} yetkisi gerektirir (JWT Bearer token).
 *
 * <h3>Tipik Akış</h3>
 * <pre>
 * 1. Mobil: Google Play'den abonelik satın al → purchaseToken al
 * 2. Mobil: POST /parent/billing/verify  { productId, purchaseToken, packageName }
 * 3. Backend: Google Play API'ye doğrulama isteği gönder
 * 4. Backend: Doğrulanan aboneliği DB'ye kaydet
 * 5. Mobil: GET /parent/billing/status → premium=true ise özellikleri aç
 * </pre>
 */
@RestController
@RequestMapping("/parent/subscription")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Google Play abonelik doğrulama ve durum sorgulama")
@SecurityRequirement(name = "Bearer Authentication")
public class BillingController {

    private final SubscriptionService subscriptionService;

    /**
     * Google Play satın alma tokenini doğrular ve aboneliği kaydeder.
     *
     * <p><b>Güvenlik:</b>
     * <ul>
     *   <li>Aynı {@code purchaseToken} ikinci kez gönderilirse {@code 409 Conflict} döner
     *       (replay attack koruması).</li>
     *   <li>Token, Google Play Developer API üzerinden doğrulanır
     *       ({@code skip-verification=false} iken).</li>
     *   <li>Paket adı backend yapılandırmasıyla eşleşmezse {@code 400 Bad Request} döner.</li>
     * </ul>
     *
     * @param req  mobil uygulamadan gelen doğrulama isteği
     * @param auth JWT kimlik bilgisi
     * @return doğrulanmış abonelik durum bilgisi
     */
    @PostMapping("/verify-purchase")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Satın alma tokenini doğrula",
            description = """
                    Google Play'den alınan purchase token'ı Google Play Developer API
                    aracılığıyla doğrular ve aboneliği kaydeder.
                    Aynı token iki kez gönderilemez (replay attack koruması).
                    """
    )
    public SubscriptionStatusResponse verify(@RequestBody PurchaseVerifyRequest req,
                                              Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);
        return subscriptionService.verifyAndSave(parentId, req);
    }

    /**
     * Ebeveynin güncel abonelik durumunu döner.
     *
     * <p>Abonelik süresi geçmişse otomatik olarak {@code EXPIRED} işaretlenir.
     * Hiç abonelik yoksa {@code premium=false} döner.
     *
     * @param auth JWT kimlik bilgisi
     * @return güncel abonelik durum bilgisi
     */
    @GetMapping("/status")
    @Operation(
            summary = "Abonelik durumunu sorgula",
            description = """
                    Ebeveynin en son abonelik kaydını döner.
                    Süresi geçmiş abonelikler otomatik EXPIRED olarak güncellenir.
                    Abonelik yoksa premium=false döner.
                    """
    )
    public SubscriptionStatusResponse status(Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);
        return subscriptionService.getStatus(parentId);
    }

    /**
     * Premium üyelik hızlı kontrol — mobil uygulama splash/startup için.
     *
     * @param auth JWT kimlik bilgisi
     * @return {@code { "premium": true/false }}
     */
    @GetMapping("/is-premium")
    @Operation(summary = "Premium kontrolü", description = "Abonelik aktif mi? Evet/hayır döner.")
    public Map<String, Boolean> isPremium(Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);
        return Map.of("premium", subscriptionService.isPremium(parentId));
    }
}
