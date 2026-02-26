package com.safekid.billing.service;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.safekid.billing.config.BillingConfig;
import com.safekid.billing.dto.GooglePlaySubscriptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Google Play Developer API üzerinden abonelik token doğrulaması yapar.
 *
 * <h3>Güvenlik</h3>
 * <ul>
 *   <li><b>Token Sahteciliği:</b> Token doğrudan Google'a gönderilir;
 *       sahte tokenlar Google'dan {@code 404} alır ve reddedilir.</li>
 *   <li><b>Skip Verification:</b> Yalnızca {@code billing.google.skip-verification=true}
 *       iken test ortamında mock yanıt üretilir. Prod'da kesinlikle {@code false}
 *       olmalıdır.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlayVerificationService {

    private static final String PLAY_API_BASE =
            "https://androidpublisher.googleapis.com/androidpublisher/v3/applications";

    private static final List<String> SCOPES =
            List.of("https://www.googleapis.com/auth/androidpublisher");

    private final BillingConfig billingConfig;
    private final ResourceLoader resourceLoader;
    private final RestTemplate restTemplate;

    /**
     * Google Play'den gelen satın alma tokenini doğrular.
     *
     * <p>{@code skip-verification=true} ise Google API çağrısı atlanır;
     * test amaçlı sahte bir aktif abonelik yanıtı döner.
     *
     * @param packageName  uygulamanın paket adı
     * @param productId    Google Play ürün kimliği
     * @param purchaseToken satın alma tokeni
     * @return Google Play API'den (veya mock'tan) gelen abonelik bilgisi
     * @throws ResponseStatusException token geçersizse veya API erişimi başarısızsa
     */
    public GooglePlaySubscriptionResponse verify(String packageName,
                                                  String productId,
                                                  String purchaseToken) {
        if (billingConfig.isSkipVerification()) {
            log.warn("skip-verification=true — Google Play API atlanıyor (SADECE TEST)");
            return buildMockResponse();
        }

        try {
            String accessToken = getAccessToken();
            String url = String.format("%s/%s/purchases/subscriptions/%s/tokens/%s",
                    PLAY_API_BASE, packageName, productId, purchaseToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<GooglePlaySubscriptionResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    GooglePlaySubscriptionResponse.class);

            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Geçersiz purchase token — Google Play kaydı bulunamadı");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Google Play API yetkilendirme hatası: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Billing servis hesabı yapılandırması hatalı");
        } catch (IOException e) {
            log.error("Servis hesabı JSON okunamadı: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Billing yapılandırma hatası");
        }
    }

    /**
     * Servis hesabı JSON'ından OAuth2 access token üretir.
     *
     * @return Google API erişim tokeni
     * @throws IOException servis hesabı dosyası okunamazsa
     */
    private String getAccessToken() throws IOException {
        try (InputStream is = resourceLoader
                .getResource(billingConfig.getServiceAccountJson())
                .getInputStream()) {

            ServiceAccountCredentials credentials = (ServiceAccountCredentials)
                    ServiceAccountCredentials.fromStream(is).createScoped(SCOPES);

            return credentials.refreshAccessToken().getTokenValue();
        }
    }

    /**
     * Test ortamı için sahte aktif abonelik yanıtı üretir.
     * {@code skip-verification=true} olduğunda kullanılır.
     */
    private GooglePlaySubscriptionResponse buildMockResponse() {
        long now = System.currentTimeMillis();
        long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;

        GooglePlaySubscriptionResponse mock = new GooglePlaySubscriptionResponse();
        mock.setStartTimeMillis(String.valueOf(now));
        mock.setExpiryTimeMillis(String.valueOf(now + thirtyDaysMs));
        mock.setAutoRenewing(true);
        mock.setOrderId("TEST_ORDER_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        mock.setPaymentState(1);
        return mock;
    }
}
