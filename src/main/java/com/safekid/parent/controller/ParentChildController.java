package com.safekid.parent.controller;

import com.safekid.config.SecurityUtils;
import com.safekid.parent.dto.ChildResponseDTO;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.repository.ChildRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/children/all")
public class ParentChildController {

    private final ChildRepository childRepository;

    public ParentChildController(ChildRepository childRepository) {
        this.childRepository = childRepository;
    }

    @GetMapping
    public List<ChildResponseDTO> getMyChildren(Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);

        List<ChildEntity> children = childRepository.findAllByParent_EbeveynUniqueId(parentId);

        return children.stream()
                .map(ChildResponseDTO::fromEntity)
                .toList();
    }
}
