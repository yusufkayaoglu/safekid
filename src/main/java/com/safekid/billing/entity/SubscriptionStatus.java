package com.safekid.billing.entity;

/**
 * Abonelik durumu.
 *
 * <ul>
 *   <li>{@link #ACTIVE}    – Abonelik geçerli ve süresi dolmamış.</li>
 *   <li>{@link #EXPIRED}   – Abonelik süresi dolmuş, yenilenmemiş.</li>
 *   <li>{@link #CANCELLED} – Kullanıcı iptal etmiş; süre bitene kadar geçerli.</li>
 *   <li>{@link #PENDING}   – Ödeme işlemi henüz tamamlanmamış.</li>
 * </ul>
 */
public enum SubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED,
    PENDING
}
