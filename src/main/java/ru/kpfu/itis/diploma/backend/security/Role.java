package ru.kpfu.itis.diploma.backend.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Getter
public enum Role {
    ADMIN, OPERATOR;

    private static final String ROLE_PREFIX = "ROLE_";

    private GrantedAuthority authority = new SimpleGrantedAuthority(ROLE_PREFIX + this.name());
}
