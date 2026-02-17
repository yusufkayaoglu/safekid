package com.safekid.jwt;

import com.safekid.auth.userprincipal.ParentPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) { this.jwtService = jwtService; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/register")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/verify-email")
                || path.startsWith("/auth/resend-verification")
                || path.startsWith("/auth/forgot-password")
                || path.startsWith("/auth/reset-password")
                || path.startsWith("/auth/child/qr-login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);

        try {
            Claims c = jwtService.parse(token).getBody();
            String type = c.get("type", String.class);

            if (type == null) {
                type = (c.get("ebeveynUniqueId") != null) ? "PARENT" : null;
            }

            if ("PARENT".equals(type)) {
                String parentId = c.get("ebeveynUniqueId", String.class);

                var auth = new UsernamePasswordAuthenticationToken(
                        "parent:" + parentId,
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_PARENT"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

            } else if ("CHILD".equals(type)) {
                String childId = c.get("cocukUniqueId", String.class);
                if (childId == null) childId = c.getSubject();

                var auth = new UsernamePasswordAuthenticationToken(
                        "child:" + childId,
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_CHILD"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                SecurityContextHolder.clearContext();
            }

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(req, res);
    }

}
