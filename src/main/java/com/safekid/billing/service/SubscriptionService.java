package com.safekid.billing.service;

import com.safekid.auth.entity.ParentEntity;
import com.safekid.auth.repository.ParentRepository;
import com.safekid.billing.config.BillingConfig;
import com.safekid.billing.dto.GooglePlaySubscriptionResponse;
import com.safekid.billing.dto.PurchaseVerifyRequest;
import com.safekid.billing.dto.SubscriptionStatusResponse;
import com.safekid.billing.entity.SubscriptionEntity;
import com.safekid.billing.entity.SubscriptionStatus;
import com.safekid.billing.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

/**
 * Google Play abonelik satın alma doğrulama ve durum sorgulama iş mantığı.
 *
 * <h3>Güvenlik Önlemleri</h3>
 * <ol>
 *   <li><b>Replay Attack:</b> {@code purchaseToken} veritabanında {@code UNIQUE}
 *       kısıtlıdır. Aynı token ikinci kez gönderilirse {@code 409 Conflict} döner.</li>
 *   <li><b>Token Sahteciliği:</b> Token, {@link GooglePlayVerificationService} aracılığıyla
 *       Google Play Developer API'ye gönderilir. Sahte tokenlar Google'dan 404 alır.</li>
 *   <li><b>Paket Adı Doğrulama:</b> İstekteki {@code packageName},
 *       {@code billing.google.package-name} yapılandırmasıyla karşılaştırılır.</li>
 *   <li><b>Sahiplik Kontrolü:</b> Abonelik durumu yalnızca token sahibi ebeveyne döner.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepo;
    private final ParentRepository parentRepo;
    private final GooglePlayVerificationService playVerificationService;
    private final BillingConfig billingConfig;

    @Value("${safekid.admin.email}")
    private String adminEmail;

    /**
     * Mobil uygulamadan gelen satın alma tokenini doğrular ve kaydeder.
     *
     * <p>İşlem adımları:
     * <ol>
     *   <li>Paket adı backend yapılandırmasıyla eşleşiyor mu kontrol et.</li>
     *   <li>Token daha önce kullanılmış mı kontrol et (replay saldırı koruması).</li>
     *   <li>Google Play Developer API'ye token gönder ve doğrula.</li>
     *   <li>Doğrulanan aboneliği veritabanına kaydet.</li>
     * </ol>
     *
     * @param parentId JWT'den çıkarılan ebeveyn kimliği
     * @param req      mobilden gelen doğrulama isteği
     * @return doğrulanmış abonelik durum yanıtı
     * @throws ResponseStatusException paket adı yanlışsa (400),
     *                                 token zaten kullanılmışsa (409),
     *                                 token geçersizse (400)
     */
    @Transactional
    public SubscriptionStatusResponse verifyAndSave(String parentId, PurchaseVerifyRequest req) {

        // 1. Paket adı doğrulama
        if (!billingConfig.getPackageName().equals(req.packageName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Geçersiz paket adı");
        }

        // 2. Replay attack koruması — aynı token daha önce kullanılmış mı?
        if (subscriptionRepo.existsByPurchaseToken(req.purchaseToken())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bu purchase token daha önce kullanılmış");
        }

        // 3. Google Play API ile token doğrulama
        GooglePlaySubscriptionResponse playResponse = playVerificationService.verify(
                req.packageName(), req.productId(), req.purchaseToken());

        // 4. Expiry kontrolü → status belirleme
        long expiryMillis = parseLong(playResponse.getExpiryTimeMillis());
        boolean expired = expiryMillis > 0 && expiryMillis < System.currentTimeMillis();
        SubscriptionStatus status = resolveStatus(playResponse, expired);

        // 5. Kaydet
        ParentEntity parent = parentRepo.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ebeveyn bulunamadı"));

        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setParent(parent);
        entity.setProductId(req.productId());
        entity.setPurchaseToken(req.purchaseToken());
        entity.setOrderId(playResponse.getOrderId());
        entity.setPackageName(req.packageName());
        entity.setStartTimeMillis(parseLong(playResponse.getStartTimeMillis()));
        entity.setExpiryTimeMillis(expiryMillis);
        entity.setAutoRenewing(playResponse.getAutoRenewing());
        entity.setStatus(status);

        subscriptionRepo.save(entity);

        log.info("Abonelik doğrulandı: parent={} product={} status={}", parentId, req.productId(), status);

        return toResponse(entity);
    }

    /**
     * Ebeveynin güncel abonelik durumunu döner.
     *
     * <p>Aktif abonelik varsa expiry zamanı kontrol edilir; süresi geçmişse
     * veritabanı kaydı {@link SubscriptionStatus#EXPIRED} olarak güncellenir.
     *
     * @param parentId JWT'den çıkarılan ebeveyn kimliği
     * @return abonelik durum yanıtı; abonelik yoksa {@code premium=false} döner
     */
    @Transactional
    public SubscriptionStatusResponse getStatus(String parentId) {

        if (isAdmin(parentId)) {
            return new SubscriptionStatusResponse(true, SubscriptionStatus.ACTIVE,
                    "admin", null, true, Instant.now());
        }

        Optional<SubscriptionEntity> latest = subscriptionRepo.findLatestByParent(parentId);

        if (latest.isEmpty()) {
            return new SubscriptionStatusResponse(false, null, null, null, null, null);
        }

        SubscriptionEntity sub = latest.get();

        // Süresi geçmişse DB'yi güncelle
        if (sub.getStatus() == SubscriptionStatus.ACTIVE
                && sub.getExpiryTimeMillis() != null
                && sub.getExpiryTimeMillis() < System.currentTimeMillis()) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepo.save(sub);
        }

        return toResponse(sub);
    }

    /**
     * Ebeveynin premium üye olup olmadığını hızlıca kontrol eder.
     *
     * @param parentId ebeveyn kimliği
     * @return abonelik aktif ve süresi dolmamışsa {@code true}
     */
    public boolean isPremium(String parentId) {
        if (isAdmin(parentId)) return true;
        return subscriptionRepo
                .findLatestByParentAndStatus(parentId, SubscriptionStatus.ACTIVE)
                .map(s -> s.getExpiryTimeMillis() != null
                        && s.getExpiryTimeMillis() > System.currentTimeMillis())
                .orElse(false);
    }

    // ─── Yardımcı metotlar ───────────────────────────────────────────────────

    private boolean isAdmin(String parentId) {
        return parentRepo.findById(parentId)
                .map(p -> adminEmail.equalsIgnoreCase(p.getEbeveynMailAdres()))
                .orElse(false);
    }

    private SubscriptionStatus resolveStatus(GooglePlaySubscriptionResponse r, boolean expired) {
        if (expired) return SubscriptionStatus.EXPIRED;
        if (r.getCancelReason() != null) return SubscriptionStatus.CANCELLED;
        if (r.getPaymentState() != null && r.getPaymentState() == 0) return SubscriptionStatus.PENDING;
        return SubscriptionStatus.ACTIVE;
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return 0L; }
    }

    private SubscriptionStatusResponse toResponse(SubscriptionEntity s) {
        boolean premium = s.getStatus() == SubscriptionStatus.ACTIVE
                && s.getExpiryTimeMillis() != null
                && s.getExpiryTimeMillis() > System.currentTimeMillis();

        return new SubscriptionStatusResponse(
                premium,
                s.getStatus(),
                s.getProductId(),
                s.getExpiryTimeMillis(),
                s.getAutoRenewing(),
                s.getVerifiedAt()
        );
    }
}
