package com.safekid.auth.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
public class EmailService {

    private final String apiKey;
    private final String fromEmail;
    private static final SecureRandom RANDOM = new SecureRandom();

    public EmailService(
            @Value("${sendgrid.api-key}") String apiKey,
            @Value("${sendgrid.from-email}") String fromEmail) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
    }

    public String generateVerificationCode() {
        int code = 100_000 + RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }

    @Async
    public void sendVerificationEmail(String to, String code) {
        String body = "Merhaba,\n\nEmail doğrulama kodunuz: " + code
                + "\n\nBu kod 15 dakika geçerlidir.\n\nSafeKid";
        send(to, "SafeKid - Email Doğrulama Kodu", body);
    }

    @Async
    public void sendPasswordResetEmail(String to, String code) {
        String body = "Merhaba,\n\nŞifre sıfırlama kodunuz: " + code
                + "\n\nBu kod 15 dakika geçerlidir.\n\nSafeKid";
        send(to, "SafeKid - Şifre Sıfırlama Kodu", body);
    }

    private void send(String to, String subject, String body) {
        try {
            Mail mail = new Mail(
                    new Email(fromEmail),
                    subject,
                    new Email(to),
                    new Content("text/plain", body)
            );

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = new SendGrid(apiKey).api(request);

            if (response.getStatusCode() >= 400) {
                log.error("MAIL_HATA: SendGrid {} -> {}", response.getStatusCode(), response.getBody());
            } else {
                log.info("MAIL_OK: mail gonderildi -> {} (status: {})", to, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("MAIL_HATA: {} - {}", e.getClass().getName(), e.getMessage(), e);
        }
    }
}
