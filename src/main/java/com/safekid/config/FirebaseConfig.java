package com.safekid.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    /**
     * Firebase henüz yapılandırılmamışsa (credentials.path boşsa) bean oluşturulmaz.
     * Push notification çalışmaz ama uygulama sorunsuz açılır.
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            return null;
        }

        Resource resource = new org.springframework.core.io.ClassPathResource(
                credentialsPath.replaceFirst("^classpath:", ""));

        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        return FirebaseApp.initializeApp(options);
    }
}
