package com.safekid.parent.controller;

import com.safekid.child.dto.LocationResponse;
import com.safekid.child.dto.MapChildLocation;
import com.safekid.child.service.ChildLocationService;
import com.safekid.child.sse.SseEmitterRegistry;
import com.safekid.config.SecurityUtils;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/parent/children")
public class ParentChildLocationController {

    private final ChildLocationService childLocationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    public ParentChildLocationController(ChildLocationService childLocationService,
                                         SseEmitterRegistry sseEmitterRegistry) {
        this.childLocationService = childLocationService;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @GetMapping("/{childId}/last-location")
    public LocationResponse getLastLocation(
            @PathVariable String childId,
            Authentication auth
    ) {
        String parentId = SecurityUtils.extractParentId(auth);
        return childLocationService.getLastLocationForParent(parentId, childId);
    }

    @GetMapping("/map")
    public List<MapChildLocation> getMap(Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);
        return childLocationService.getMapForParent(parentId);
    }

    @GetMapping(value = "/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLocations(Authentication auth) {
        String parentId = SecurityUtils.extractParentId(auth);
        return sseEmitterRegistry.register(parentId);
    }
}
