package com.safekid.child.controller;

import com.safekid.child.dto.LocationCreateRequest;
import com.safekid.child.dto.LocationResponse;
import com.safekid.child.service.ChildLocationService;
import com.safekid.config.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/child/location")
public class ChildLocationController {

    private final ChildLocationService service;

    public ChildLocationController(ChildLocationService service) {
        this.service = service;
    }

    @PostMapping
    public LocationResponse pushLocation(@RequestBody LocationCreateRequest req, Authentication auth) {
        String childId = SecurityUtils.extractChildId(auth);
        return service.saveLocation(childId, req);
    }
}
