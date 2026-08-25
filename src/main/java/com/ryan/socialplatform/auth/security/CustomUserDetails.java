// auth/security/CustomUserDetails.java
package com.ryan.socialplatform.auth.security;

import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserCredentials;
import com.ryan.socialplatform.user.enums.Status;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final Status status;
    private final Instant lockedUntil;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(UserCredentials credentials, User user, List<String> roles) {
        this.userId = user.getId();
        this.username = credentials.getEmail() != null
                ? credentials.getEmail()
                : credentials.getPhoneNumber();
        this.passwordHash = credentials.getPasswordHash();
        this.status = user.getStatus();
        this.lockedUntil = credentials.getLockedUntil();
        this.authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(Instant.now());
    }

    @Override
    public boolean isEnabled() {
        return status == Status.ACTIVE;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}