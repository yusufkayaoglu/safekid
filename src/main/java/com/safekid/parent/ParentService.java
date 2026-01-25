package com.safekid.parent;

import com.safekid.auth.util.IdGenerator;
import com.safekid.auth.entity.ParentEntity;
import com.safekid.auth.userprincipal.ParentPrincipalService;
import com.safekid.auth.entity.ChildEntity;
import com.safekid.auth.repository.ChildRepository;
import com.safekid.parent.dto.ChildCreateRequest;
import com.safekid.parent.dto.ChildResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ParentService {

    private final ParentPrincipalService principalService;
    private final ChildRepository childRepository;

    public ParentService(ParentPrincipalService principalService, ChildRepository childRepository) {
        this.principalService = principalService;
        this.childRepository = childRepository;
    }

    @Transactional
    public ChildResponse addChild(ChildCreateRequest req) {
        ParentEntity parent = principalService.getCurrentParentOrThrow();

        ChildEntity child = new ChildEntity();
        child.setCocukUniqueId(IdGenerator.newId());
        child.setCocukAdi(req.cocukAdi());
        child.setCocukSoyadi(req.cocukSoyadi());
        child.setCocukTelefonNo(req.cocukTelefonNo());
        child.setCocukMail(req.cocukMail());
        child.setParent(parent);

        childRepository.save(child);

        return new ChildResponse(
                child.getCocukUniqueId(),
                child.getCocukAdi(),
                child.getCocukSoyadi(),
                child.getCocukTelefonNo(),
                child.getCocukMail()
        );
    }
}
