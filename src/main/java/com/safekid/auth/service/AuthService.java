package com.safekid.auth.service;

import com.safekid.auth.util.IdGenerator;
import com.safekid.auth.repository.ParentRepository;
import com.safekid.auth.dto.LoginRequest;
import com.safekid.auth.dto.LoginResponse;
import com.safekid.auth.dto.RegisterRequest;
import com.safekid.auth.dto.VerifyRequest;
import com.safekid.auth.entity.ParentEntity;
import com.safekid.jwt.JwtService;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.repository.ChildRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@AllArgsConstructor
public class AuthService {
    private final ParentRepository parentRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ChildRepository childRepository;
    private final EmailService emailService;

    @Transactional
    public void register(RegisterRequest req) {
        if (parentRepo.existsByEbeveynUserCode(req.ebeveynUserCode()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UserCode zaten var");

        ParentEntity p = new ParentEntity();
        p.setEbeveynAdi(req.ebeveynAdi());
        p.setEbeveynSoyadi(req.ebeveynSoyadi());
        p.setEbeveynUserCode(req.ebeveynUserCode());
        p.setEbeveynMailAdres(req.ebeveynMailAdres());
        p.setEbeveynPassword(passwordEncoder.encode(req.ebeveynPassword()));
        p.setEbeveynAdres(req.ebeveynAdres());
        p.setEbeveynIsAdresi(req.ebeveynIsAdresi());

        String code = emailService.generateVerificationCode();
        p.setEbeveynDogrulamaKodu(code);
        p.setDogrulamaKoduSonKullanma(Instant.now().plus(15, ChronoUnit.MINUTES));
        p.setEbeveynMailDogrulandi(false);

        parentRepo.save(p);

        try {
            emailService.sendVerificationEmail(req.ebeveynMailAdres(), code);
        } catch (Exception e) {
            // Kayıt yine tamamlansın, kullanıcı sonra resend yapabilir
            System.err.println("Mail gönderilemedi: " + e.getMessage());
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        ParentEntity p = parentRepo.findByEbeveynMailAdres(req.ebeveynMailAdres())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(req.ebeveynPassword(), p.getEbeveynPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Şifre hatalı");

        if (!p.isEbeveynMailDogrulandi())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email doğrulanmamış. Lütfen email adresinize gönderilen kodu onaylayın.");

        String sessionToken = "session-" + IdGenerator.newId();
        p.setEbeveynToken(sessionToken);
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
    public void verifyEmail(VerifyRequest req) {
        ParentEntity p = parentRepo.findByEbeveynMailAdres(req.ebeveynMailAdres())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (p.isEbeveynMailDogrulandi())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email zaten doğrulanmış");

        if (p.getEbeveynDogrulamaKodu() == null || !p.getEbeveynDogrulamaKodu().equals(req.dogrulamaKodu()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doğrulama kodu hatalı");

        if (p.getDogrulamaKoduSonKullanma() == null || Instant.now().isAfter(p.getDogrulamaKoduSonKullanma()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doğrulama kodunun süresi dolmuş. Yeni kod talep edin.");

        p.setEbeveynMailDogrulandi(true);
        p.setEbeveynDogrulamaKodu(null);
        p.setDogrulamaKoduSonKullanma(null);
        parentRepo.save(p);
    }

    @Transactional
    public void resendVerification(String ebeveynMailAdres) {
        ParentEntity p = parentRepo.findByEbeveynMailAdres(ebeveynMailAdres)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (p.isEbeveynMailDogrulandi())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email zaten doğrulanmış");

        String code = emailService.generateVerificationCode();
        p.setEbeveynDogrulamaKodu(code);
        p.setDogrulamaKoduSonKullanma(Instant.now().plus(15, ChronoUnit.MINUTES));
        parentRepo.save(p);

        emailService.sendVerificationEmail(ebeveynMailAdres, code);
    }

    @Transactional
    public void forgotPassword(String email) {
        ParentEntity p = parentRepo.findByEbeveynMailAdres(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        String code = emailService.generateVerificationCode();
        p.setSifreSifirlamaKodu(code);
        p.setSifreSifirlamaKoduSonKullanma(Instant.now().plus(15, ChronoUnit.MINUTES));
        parentRepo.save(p);

        emailService.sendPasswordResetEmail(email, code);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        ParentEntity p = parentRepo.findByEbeveynMailAdres(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (p.getSifreSifirlamaKodu() == null || !p.getSifreSifirlamaKodu().equals(code))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sıfırlama kodu hatalı");

        if (p.getSifreSifirlamaKoduSonKullanma() == null || Instant.now().isAfter(p.getSifreSifirlamaKoduSonKullanma()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sıfırlama kodunun süresi dolmuş. Yeni kod talep edin.");

        p.setEbeveynPassword(passwordEncoder.encode(newPassword));
        p.setSifreSifirlamaKodu(null);
        p.setSifreSifirlamaKoduSonKullanma(null);
        parentRepo.save(p);
    }

    @Transactional
    public void logout(String parentId) {
        ParentEntity p = parentRepo.findById(parentId).orElseThrow();
        p.setEbeveynToken(null);
        parentRepo.save(p);
    }

    @Transactional
    public LoginResponse childQrLogin(String cocukUniqueId) {
        ChildEntity child = childRepository
                .findByCocukUniqueId(cocukUniqueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));

        JwtService.JwtPair token = jwtService.issueChildToken(child);

        return new LoginResponse(
                token.token(),
                token.expiresAt(),
                null,
                child.getCocukAdi(),
                child.getCocukSoyadi()
        );
    }
}
