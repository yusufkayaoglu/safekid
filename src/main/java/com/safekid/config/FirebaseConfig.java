package com.safekid.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${FIREBASE_CREDENTIALS_BASE64:}")
    private String credentialsBase64;

    /**
     * Önce FIREBASE_CREDENTIALS_BASE64 env var'ına bakar (Railway/prod).
     * Yoksa firebase.credentials.path'e bakar (lokal geliştirme).
     * İkisi de yoksa Firebase başlatılmaz, uygulama sorunsuz açılır.
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        InputStream credentialsStream = null;

        // @Value ile gelmediyse System.getenv'den direkt oku
        String base64 = (credentialsBase64 != null && !credentialsBase64.isBlank())
                ? credentialsBase64
                : System.getenv("FIREBASE_CREDENTIALS_BASE64");

        if (base64 != null && !base64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(base64.trim());
            credentialsStream = new ByteArrayInputStream(decoded);
        } else if (credentialsPath != null && !credentialsPath.isBlank()) {
            Resource resource = new org.springframework.core.io.ClassPathResource(
                    credentialsPath.replaceFirst("^classpath:", ""));
            if (resource.exists()) {
                credentialsStream = resource.getInputStream();
            }
        }

        if (credentialsStream == null) {
            log.info("Firebase credentials bulunamadı, Firebase başlatılmıyor.");
            return null;
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        return FirebaseApp.initializeApp(options);
    }
}
