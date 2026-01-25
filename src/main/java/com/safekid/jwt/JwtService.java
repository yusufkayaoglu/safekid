package com.safekid.jwt;

import com.safekid.auth.entity.ParentEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    public record JwtPair(String token, Instant expiresAt) {}

    private final SecretKey key; // HMAC
    private final Duration ttl = Duration.ofHours(8);

    public JwtService(@Value("${security.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtPair issueParentToken(ParentEntity p, String sessionToken) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);

        String jwt = Jwts.builder()
                .setSubject("PARENT")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("ebeveynUniqueId", p.getEbeveynUniqueId())
                .claim("ebeveynAdi", p.getEbeveynAdi())
                .claim("ebeveynSoyadi", p.getEbeveynSoyadi())
                .claim("ebeveynToken", sessionToken)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return new JwtPair(jwt, exp);
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
