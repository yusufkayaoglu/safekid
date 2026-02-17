package com.safekid.parent.service;

import com.safekid.auth.util.IdGenerator;
import com.safekid.auth.entity.ParentEntity;
import com.safekid.auth.userprincipal.ParentPrincipalService;
import com.safekid.child.dto.LocationResponse;
import com.safekid.child.service.ChildLocationService;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.dto.ChildCreateRequest;
import com.safekid.parent.dto.ChildResponse;
import com.safekid.parent.repository.ChildRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ParentService {

    private final ParentPrincipalService principalService;
    private final ChildRepository childRepository;
    private final ChildLocationService childLocationService;


    public ParentService(ParentPrincipalService principalService, ChildRepository childRepository,ChildLocationService childLocationService) {
        this.principalService = principalService;
        this.childRepository = childRepository;
        this.childLocationService = childLocationService;
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

    public LocationResponse getChildLastLocation(String childId) {
        ParentEntity parent = principalService.getCurrentParentOrThrow();

        ChildEntity child = childRepository.findByCocukUniqueId(childId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));

        if (!child.getParent().getEbeveynUniqueId().equals(parent.getEbeveynUniqueId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu çocuk sana ait değil");
        }

        return childLocationService.getLastLocationForParent(
                parent.getEbeveynUniqueId(),
                childId
        );

    }

}
