package com.safekid.parent.service;

import com.safekid.auth.entity.ParentEntity;
import com.safekid.auth.repository.ParentRepository;
import com.safekid.config.SecurityUtils;
import com.safekid.parent.dto.ChildCreateRequest;
import com.safekid.parent.dto.ChildResponse;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.repository.ChildRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

import com.safekid.auth.util.IdGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentRepository parentRepository;

    private String currentParentId() {
        return SecurityUtils.extractParentId(
                SecurityContextHolder.getContext().getAuthentication());
    }




    @Transactional
    public ChildResponse addChild(ChildCreateRequest req) {
        String parentId = currentParentId();

        ParentEntity parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent not found"));

        if (req.cocukAdi() == null || req.cocukAdi().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cocukAdi zorunlu");
        if (req.cocukSoyadi() == null || req.cocukSoyadi().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cocukSoyadi zorunlu");

        ChildEntity c = new ChildEntity();
        c.setCocukUniqueId(IdGenerator.newId());
        c.setCocukAdi(req.cocukAdi().trim());
        c.setCocukSoyadi(req.cocukSoyadi().trim());
        c.setParent(parent);

        childRepository.save(c);

        return toResponse(c);
    }

    public List<ChildResponse> listMyChildren() {
        String parentId = currentParentId();
        return childRepository.findAllByParent_EbeveynUniqueId(parentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ChildResponse getChild(String childId) {
        String parentId = currentParentId();
        ChildEntity c = childRepository.findByCocukUniqueIdAndParent_EbeveynUniqueId(childId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        return toResponse(c);
    }

    @Transactional
    public void deleteChild(String childId) {
        String parentId = currentParentId();
        ChildEntity c = childRepository.findByCocukUniqueIdAndParent_EbeveynUniqueId(childId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        childRepository.delete(c);
    }

    private ChildResponse toResponse(ChildEntity c) {
        return new ChildResponse(
                c.getCocukUniqueId(),
                c.getCocukAdi(),
                c.getCocukSoyadi()
        );
    }

    public List<ChildEntity> getChildrenOfParent(String parentId) {
        return childRepository.findAllByParent_EbeveynUniqueId(parentId);
    }

}
