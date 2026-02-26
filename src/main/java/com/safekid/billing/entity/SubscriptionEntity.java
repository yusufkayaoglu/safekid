package com.safekid.billing.entity;

import com.safekid.auth.entity.ParentEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Ebeveyne ait Google Play abonelik kaydı.
 *
 * <p><b>Replay Attack Koruması:</b> {@code purchaseToken} kolonu {@code UNIQUE}
 * kısıtlamasına sahiptir. Aynı token ikinci kez gönderilirse DB insert çakışması
 * oluşur ve servis katmanında {@code 409 Conflict} döner.
 *
 * <p><b>Token Sahteciliği Koruması:</b> Token, Google Play Developer API üzerinden
 * doğrulanır ({@code billing.google.skip-verification=false}). Sahte bir token
 * Google'dan 404 yanıtı alır ve reddedilir.
 */
@Getter
@Setter
@Entity
@Table(name = "abonelik")
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Aboneliği satın alan ebeveyn.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ebeveyn_unique_id", nullable = false)
    private ParentEntity parent;

    /**
     * Google Play ürün kimliği.
     * Örnek: {@code safekid_premium_monthly}, {@code safekid_premium_yearly}
     */
    @Column(name = "product_id", nullable = false)
    private String productId;

    /**
     * Google Play satın alma tokeni. Replay saldırılarına karşı UNIQUE.
     * Her başarılı satın alma için Google farklı bir token üretir.
     */
    @Column(name = "purchase_token", unique = true, nullable = false, length = 512)
    private String purchaseToken;

    /**
     * Google Play sipariş kimliği (örn. {@code GPA.1234-5678-9012-34567}).
     */
    @Column(name = "order_id")
    private String orderId;

    /**
     * İstek gönderilirken kullanılan paket adı.
     * {@code billing.google.package-name} ile karşılaştırılarak doğrulanır.
     */
    @Column(name = "package_name", nullable = false)
    private String packageName;

    /**
     * Abonelik başlangıç zamanı (epoch milisaniye, Google API'den gelir).
     */
    @Column(name = "start_time_millis")
    private Long startTimeMillis;

    /**
     * Abonelik bitiş zamanı (epoch milisaniye, Google API'den gelir).
     * Bu değer geçmişte ise abonelik {@link SubscriptionStatus#EXPIRED} kabul edilir.
     */
    @Column(name = "expiry_time_millis")
    private Long expiryTimeMillis;

    /**
     * Aboneliğin otomatik yenilenmesinin aktif olup olmadığı.
     */
    @Column(name = "auto_renewing")
    private Boolean autoRenewing;

    /**
     * Abonelik durumu. Google API yanıtına ve expiry bilgisine göre belirlenir.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    /**
     * Token'ın backend tarafından doğrulandığı zaman.
     */
    @Column(name = "verified_at", nullable = false, updatable = false)
    private Instant verifiedAt;

    @PrePersist
    void prePersist() {
        if (verifiedAt == null) verifiedAt = Instant.now();
    }
}
