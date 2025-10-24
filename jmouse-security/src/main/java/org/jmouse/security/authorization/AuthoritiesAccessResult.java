package org.jmouse.security.authorization;

import org.jmouse.security.Authority;

import java.util.Collection;
import java.util.Map;

public class AuthoritiesAccessResult extends AccessResult.AbstractAccessResult {

    public static final String AUTHORITIES_ATTRIBUTE = "AUTHORITIES_ATTRIBUTE";

    private final Collection<Authority> authorities;

    public AuthoritiesAccessResult(boolean granted, Collection<Authority> authorities) {
        super(granted, null, Map.of(AUTHORITIES_ATTRIBUTE, authorities));
        this.authorities = authorities;
    }

    public Collection<Authority> getAuthorities() {
        return authorities;
    }

    @Override
    public String toString() {
        return "%s : %s".formatted(super.toString(), authorities);
    }
}
