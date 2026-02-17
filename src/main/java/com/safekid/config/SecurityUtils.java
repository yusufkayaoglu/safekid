package com.safekid.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String extractParentId(Authentication auth) {
        String name = auth.getName();
        if (!name.startsWith("parent:")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a parent token");
        }
        return name.substring(7);
    }

    public static String extractChildId(Authentication auth) {
        String name = auth.getName();
        if (!name.startsWith("child:")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a child token");
        }
        return name.substring(6);
    }
}
