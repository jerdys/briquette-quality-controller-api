package ru.kpfu.itis.diploma.backend.security.details;

import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collection;

public class AuthorizedUser extends org.springframework.security.core.userdetails.User {
    private boolean isDeleted;
    private long id;

    public AuthorizedUser(long id,
                          String username,
                          String password,
                          boolean isDeleted,
                          Collection<GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.isDeleted = isDeleted;
        this.id = id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !isDeleted;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return super.getAuthorities();
    }

    @Override
    public String getPassword() {
        return super.getPassword();
    }

    @Override
    public String getUsername() {
        return super.getUsername();
    }

    public boolean isDeleted() {
        return this.isDeleted;
    }

    public long getId() {
        return this.id;
    }

    public boolean hasRole(String authority) {
        return getAuthorities()
                .stream()
                .anyMatch(grantedAuthority ->
                        grantedAuthority
                                .getAuthority()
                                .equals("ROLE_" + authority));
    }

    public boolean hasAnyRole(String... authority) {
        return Arrays.stream(authority)
                .anyMatch(this::hasRole);
    }
}
