package com.bank4z.backend.authservice;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final User user;

    /** Which login session this request's token belongs to. Null outside of token-authenticated requests. */
    private final UUID sessionId;

    public UserPrincipal(User user) {
        this(user, null);
    }

    public UserPrincipal(User user, UUID sessionId) {
        this.user = user;
        this.sessionId = sessionId;
    }

    public UUID getId() {
        return user.getId();
    }

    public User getUser() {
        return user;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}