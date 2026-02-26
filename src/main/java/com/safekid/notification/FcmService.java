package com.safekid.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private final FirebaseApp firebaseApp;

    @Autowired(required = false)
    public FcmService(@Nullable FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    /**
     * Ebeveynin cihazına push notification gönderir.
     * FCM token null/boş ise veya Firebase yapılandırılmamışsa sessizce atlanır.
     */
    public void sendPush(String fcmToken, String title, String body) {
        if (firebaseApp == null) {
            log.debug("Firebase yapılandırılmamış, push notification atlanıyor.");
            return;
        }
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("FCM token yok, push notification atlanıyor.");
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            FirebaseMessaging.getInstance(firebaseApp).sendAsync(message);
        } catch (Exception e) {
            log.warn("Push notification gönderilemedi: {}", e.getMessage());
        }
    }
}
