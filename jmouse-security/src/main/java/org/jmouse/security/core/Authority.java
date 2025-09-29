package org.jmouse.security.core;

import org.jmouse.core.Streamable;

import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface Authority {

    List<Authority> ANONYMOUS = List.of(Authority.of("ROLE_ANONYMOUS"));

    static Authority of(String authority) {
        return new GrantedAuthority(authority);
    }

    static Collection<Authority> ofCollection(Collection<String> authorities) {
        return Streamable.of(authorities).map(Authority::of).toList();
    }

    static Collection<Authority> ofArray(String... authorities) {
        return ofCollection(List.of(authorities));
    }

    default boolean isGranted(Authority authority) {
        return authority().equalsIgnoreCase(authority.authority());
    }

    String authority();

    record GrantedAuthority(String authority) implements Authority {
    }

}