package com.salik.contactmanagementapi.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

public class UserAuthentication extends AbstractAuthenticationToken {

    private final String userId;
    private final String username;

    public UserAuthentication(String userId, String username, Collection<SimpleGrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.username = username;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // No credentials after authentication
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    public String getUserId() {
        return userId;
    }
}