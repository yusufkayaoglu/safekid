package com.safekid.auth.controller;

import com.safekid.auth.service.AuthService;
import com.safekid.auth.dto.LoginRequest;
import com.safekid.auth.dto.LoginResponse;
import com.safekid.auth.dto.RegisterRequest;
import com.safekid.auth.userprincipal.ParentPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        ParentPrincipal principal = (ParentPrincipal) authentication.getPrincipal();
        authService.logout(principal.getParentId());
        return ResponseEntity.ok().build();
    }
}
