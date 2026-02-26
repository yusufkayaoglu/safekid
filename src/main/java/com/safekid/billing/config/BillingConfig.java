package com.safekid.billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Google Play Billing yapılandırması.
 *
 * <p>application.properties'den {@code billing.google.*} prefix'li değerleri okur.
 *
 * <pre>
 * billing.google.skip-verification=false        # Prod'da false olmalı
 * billing.google.service-account-json=classpath:google-service-account.json
 * billing.google.package-name=com.safekid.mobile
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "billing.google")
@Getter
@Setter
public class BillingConfig {

    /**
     * {@code true} ise Google Play API çağrısı atlanır, sahte aktif abonelik döner.
     * Yalnızca test/geliştirme ortamında kullanılmalıdır.
     */
    private boolean skipVerification;

    /**
     * Google Play Developer API için servis hesabı JSON dosyasının yolu.
     * Örnek: {@code classpath:google-service-account.json}
     */
    private String serviceAccountJson;

    /**
     * Uygulamanın Google Play paket adı.
     * Gelen isteklerdeki packageName bu değerle karşılaştırılır.
     */
    private String packageName;
}
