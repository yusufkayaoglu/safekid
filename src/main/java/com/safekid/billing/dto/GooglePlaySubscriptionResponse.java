package com.safekid.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Google Play Developer API'den dönen abonelik yanıtı.
 *
 * <p>Endpoint:
 * {@code GET /androidpublisher/v3/applications/{packageName}/purchases/subscriptions/{subscriptionId}/tokens/{token}}
 *
 * <p>Bilinmeyen alanlar sessizce yok sayılır ({@code @JsonIgnoreProperties}).
 *
 * @see <a href="https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptions/get">
 *      Google Play Developer API — purchases.subscriptions.get</a>
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlaySubscriptionResponse {

    /**
     * Abonelik başlangıç zamanı (epoch milisaniye, string olarak gelir).
     */
    private String startTimeMillis;

    /**
     * Abonelik bitiş zamanı (epoch milisaniye, string olarak gelir).
     * Bu zaman geçmişte ise abonelik süresi dolmuştur.
     */
    private String expiryTimeMillis;

    /**
     * Otomatik yenileme aktif mi?
     */
    private Boolean autoRenewing;

    /**
     * Google Play sipariş kimliği.
     * Yenilenen aboneliklerde {@code ..0}, {@code ..1} gibi sonek alır.
     */
    private String orderId;

    /**
     * Ödeme durumu.
     * <ul>
     *   <li>0 – Ödeme beklemede</li>
     *   <li>1 – Ödeme alındı</li>
     *   <li>2 – Ücretsiz deneme</li>
     *   <li>3 – Bekleyen harici fiyat değişikliği yükseltmesi</li>
     * </ul>
     */
    private Integer paymentState;

    /**
     * İptal nedeni (yalnızca iptal edilmiş aboneliklerde).
     * <ul>
     *   <li>0 – Kullanıcı iptal etti</li>
     *   <li>1 – Sistem iptal etti</li>
     *   <li>2 – Yeni abonelikle değiştirildi</li>
     *   <li>3 – Geliştirici iptal etti</li>
     * </ul>
     */
    private Integer cancelReason;
}
