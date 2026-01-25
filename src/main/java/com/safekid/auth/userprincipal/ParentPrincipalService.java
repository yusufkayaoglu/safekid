package com.safekid.auth.userprincipal;


import com.safekid.auth.entity.ParentEntity;
import com.safekid.auth.repository.ParentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ParentPrincipalService {

    private final ParentRepository parentRepository;

    public ParentPrincipalService(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    public ParentEntity getCurrentParentOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Authentication bulunamadı");
        }

        if (!(auth.getPrincipal() instanceof ParentPrincipal principal)) {
            throw new RuntimeException("Principal tipi ParentPrincipal değil");
        }

        String parentId = principal.getParentId();
        return parentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Ebeveyn DB'de bulunamadı: " + parentId));
    }
}