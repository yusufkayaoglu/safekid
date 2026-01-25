package com.safekid.auth.service;

import com.safekid.auth.util.IdGenerator;
import com.safekid.auth.repository.ParentRepository;
import com.safekid.auth.dto.LoginRequest;
import com.safekid.auth.dto.LoginResponse;
import com.safekid.auth.dto.RegisterRequest;
import com.safekid.auth.entity.ParentEntity;
import com.safekid.jwt.JwtService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
    private final ParentRepository parentRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest req) {
        if (parentRepo.existsByEbeveynUserCode(req.ebeveynUserCode()))
            throw new RuntimeException("UserCode zaten var");

        ParentEntity p = new ParentEntity();
        p.setEbeveynAdi(req.ebeveynAdi());
        p.setEbeveynSoyadi(req.ebeveynSoyadi());
        p.setEbeveynUserCode(req.ebeveynUserCode());
        p.setEbeveynMailAdres(req.ebeveynMailAdres());
        p.setEbeveynTelefonNumarasi(req.ebeveynTelefonNumarasi());
        p.setEbeveynPassword(passwordEncoder.encode(req.ebeveynPassword()));

        // “ebeveynToken” alanını login’de session gibi üretip yazmak daha mantıklı.
        parentRepo.save(p);
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        ParentEntity p = parentRepo.findByEbeveynUserCode(req.ebeveynUserCode())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(req.ebeveynPassword(), p.getEbeveynPassword()))
            throw new RuntimeException("Şifre hatalı");

        // session token (ebeveynToken) üret
        String sessionToken = "session-" + IdGenerator.newId();
        p.setEbeveynToken(sessionToken); // opsiyonel persist
        parentRepo.save(p);

        JwtService.JwtPair token = jwtService.issueParentToken(p, sessionToken);
        return new LoginResponse(
                token.token(),
                token.expiresAt(),
                p.getEbeveynUniqueId(),
                p.getEbeveynAdi(),
                p.getEbeveynSoyadi()
        );
    }

    @Transactional
    public void logout(String parentId) {
        // Stateless JWT’de logout = token blacklist / tokenVersion / sessionToken null vb.
        ParentEntity p = parentRepo.findById(parentId).orElseThrow();
        p.setEbeveynToken(null);
        parentRepo.save(p);
    }
}
