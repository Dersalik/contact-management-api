package com.salik.contactmanagementapi.controller;

import com.salik.contactmanagementapi.security.UserAuthentication;
import org.springframework.security.core.Authentication;

public abstract class BaseController {

    protected String extractUserId(Authentication authentication) {
        if (authentication instanceof UserAuthentication) {
            return ((UserAuthentication) authentication).getUserId();
        }
        return null;
    }
}