package com.safekid.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateVerificationCode() {
        int code = 100_000 + RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }

    @Async
    public void sendVerificationEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("SafeKid - Email Doğrulama Kodu");
            message.setText("Merhaba,\n\nEmail doğrulama kodunuz: " + code
                    + "\n\nBu kod 15 dakika geçerlidir.\n\nSafeKid");
            mailSender.send(message);
            log.info("MAIL_OK: dogrulama maili gonderildi -> {}", to);
        } catch (Exception e) {
            log.error("MAIL_HATA: {} - {}", e.getClass().getName(), e.getMessage(), e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("SafeKid - Şifre Sıfırlama Kodu");
            message.setText("Merhaba,\n\nŞifre sıfırlama kodunuz: " + code
                    + "\n\nBu kod 15 dakika geçerlidir.\n\nSafeKid");
            mailSender.send(message);
            log.info("MAIL_OK: sifre sifirlama maili gonderildi -> {}", to);
        } catch (Exception e) {
            log.error("MAIL_HATA: {} - {}", e.getClass().getName(), e.getMessage(), e);
        }
    }
}
