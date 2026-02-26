package com.safekid.billing.dto;

import com.safekid.billing.entity.SubscriptionStatus;

import java.time.Instant;

/**
 * Ebeveyne dönen abonelik durum yanıtı.
 *
 * @param premium         Abonelik aktif ve süresi dolmamışsa {@code true}.
 *                        Mobil uygulama bu değere göre premium özellikleri açar.
 * @param status          Abonelik durumu ({@link SubscriptionStatus}).
 * @param productId       Aktif ürün kimliği; abonelik yoksa {@code null}.
 * @param expiryTimeMillis Abonelik bitiş zamanı (epoch ms); yoksa {@code null}.
 * @param autoRenewing    Otomatik yenileme aktifse {@code true}.
 * @param verifiedAt      Token'ın backend tarafından son doğrulandığı zaman.
 */
public record SubscriptionStatusResponse(
        boolean premium,
        SubscriptionStatus status,
        String productId,
        Long expiryTimeMillis,
        Boolean autoRenewing,
        Instant verifiedAt
) {}
