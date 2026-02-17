package com.safekid.auth.controller;

import com.safekid.auth.dto.*;
import com.safekid.auth.service.AuthService;
import com.safekid.auth.userprincipal.ParentPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok(Map.of("message", "Kayıt başarılı. Email adresinize doğrulama kodu gönderildi."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody VerifyRequest req) {
        authService.verifyEmail(req);
        return ResponseEntity.ok(Map.of("message", "Email başarıyla doğrulandı. Artık giriş yapabilirsiniz."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody Map<String, String> body) {
        authService.resendVerification(body.get("ebeveynMailAdres"));
        return ResponseEntity.ok(Map.of("message", "Yeni doğrulama kodu email adresinize gönderildi."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.ebeveynMailAdres());
        return ResponseEntity.ok(Map.of("message", "Şifre sıfırlama kodu email adresinize gönderildi."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.ebeveynMailAdres(), req.kod(), req.yeniSifre());
        return ResponseEntity.ok(Map.of("message", "Şifreniz başarıyla güncellendi. Yeni şifrenizle giriş yapabilirsiniz."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        ParentPrincipal principal = (ParentPrincipal) authentication.getPrincipal();
        authService.logout(principal.getParentId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/child/qr-login")
    public LoginResponse childQrLogin(@RequestParam String cocukUniqueId) {
        return authService.childQrLogin(cocukUniqueId);
    }

    @GetMapping("/whoami")
    public Map<String, Object> whoami(Authentication auth) {
        if (auth == null) {
            return Map.of(
                    "authenticated", false,
                    "message", "Authentication null. Authorization header yok veya JWT parse edilemedi."
            );
        }
        return Map.of(
                "authenticated", auth.isAuthenticated(),
                "name", auth.getName(),
                "authorities", auth.getAuthorities().stream().map(a -> a.getAuthority()).toList()
        );
    }
}
