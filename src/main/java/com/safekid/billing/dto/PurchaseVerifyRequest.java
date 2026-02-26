package com.safekid.billing.dto;

/**
 * Mobil uygulamadan gelen satın alma doğrulama isteği.
 *
 * <p>Mobil uygulama Google Play'den satın alma tamamlandığında bu DTO ile
 * backend'e bildirir. Backend token'ı Google Play Developer API üzerinden doğrular.
 *
 * @param productId     Google Play ürün kimliği
 *                      (örn. {@code safekid_premium_monthly})
 * @param purchaseToken Google Play tarafından üretilen benzersiz token.
 *                      Her satın alma için farklıdır; replay saldırılarına karşı
 *                      backend'de UNIQUE olarak saklanır.
 * @param packageName   Uygulamanın paket adı (örn. {@code com.safekid.mobile}).
 *                      Backend yapılandırmasıyla karşılaştırılarak doğrulanır.
 */
public record PurchaseVerifyRequest(
        String productId,
        String purchaseToken,
        String packageName
) {}
