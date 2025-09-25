package org.jmouse.security.core;

import java.util.List;

@FunctionalInterface
public interface Authority {

    List<Authority> ANONYMOUS = List.of(Authority.of("ROLE_ANONYMOUS"));

    static Authority of(String authority) {
        return new GrantedAuthority(authority);
    }

    default boolean isGranted(Authority authority) {
        return authority().equalsIgnoreCase(authority.authority());
    }

    String authority();

    record GrantedAuthority(String authority) implements Authority {
    }

}