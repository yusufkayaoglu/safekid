package com.safekid.parent.controller;

import com.safekid.child.dto.LocationResponse;
import com.safekid.child.dto.MapChildLocation;
import com.safekid.child.service.ChildLocationService;
import com.safekid.child.sse.SseEmitterRegistry;
import com.safekid.config.SecurityUtils;
import com.safekid.parent.dto.FcmTokenRequest;
import com.safekid.parent.service.ParentService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ParentService parentService;

    public ParentChildLocationController(ChildLocationService childLocationService,
                                         SseEmitterRegistry sseEmitterRegistry,
                                         ParentService parentService) {
        this.childLocationService = childLocationService;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.parentService = parentService;
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
    public SseEmitter streamLocations(Authentication auth, HttpServletRequest request) {
        // Client bağlantısı koptuğunda Tomcat aynı URL'ye DispatcherType.ERROR ile tekrar
        // gelir. startAsync() ERROR dispatch'te çalışmaz → null dönerek atla.
        if (request.getDispatcherType() == DispatcherType.ERROR) {
            return null;
        }
        String parentId = SecurityUtils.extractParentId(auth);
        return sseEmitterRegistry.register(parentId);
    }

    @PutMapping("/fcm-token")
    public void updateFcmToken(
            @RequestBody FcmTokenRequest request,
            Authentication auth
    ) {
        String parentId = SecurityUtils.extractParentId(auth);
        parentService.updateFcmToken(parentId, request.token());
    }
}
