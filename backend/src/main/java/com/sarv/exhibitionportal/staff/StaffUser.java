package com.sarv.exhibitionportal.staff;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class StaffUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String displayName;
    private final String passwordHash;
    private final Set<String> roleCodes;
    private final boolean active;

    public StaffUser(
            UUID id,
            String email,
            String displayName,
            String passwordHash,
            Set<String> roleCodes,
            boolean active
    ) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.roleCodes = Set.copyOf(roleCodes);
        this.active = active;
    }

    public UUID id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Set<String> roleCodes() {
        return roleCodes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleCodes.stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
