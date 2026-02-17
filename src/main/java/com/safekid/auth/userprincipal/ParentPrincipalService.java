package com.safekid.auth.userprincipal;

import com.safekid.auth.entity.ParentEntity;
import com.safekid.auth.repository.ParentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ParentPrincipalService {

    private final ParentRepository parentRepository;

    public ParentPrincipalService(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    public ParentEntity getCurrentParentOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication bulunamadı");
        }

        if (!(auth.getPrincipal() instanceof ParentPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Parent token gerekli");
        }

        String parentId = principal.getParentId();
        return parentRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ebeveyn bulunamadı"));
    }
}
