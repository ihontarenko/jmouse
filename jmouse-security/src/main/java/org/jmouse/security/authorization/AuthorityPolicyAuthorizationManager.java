package org.jmouse.security.authorization;

import org.jmouse.core.Streamable;
import org.jmouse.security.core.Authentication;

import java.util.Collection;
import java.util.Set;

public final class AuthorityPolicyAuthorizationManager<T> implements AuthorizationManager<T> {

    private final AuthoritiesAuthorizationManager authorizationManager = new AuthoritiesAuthorizationManager();
    private final Collection<String>              authorities;

    public AuthorityPolicyAuthorizationManager(Collection<String> authorities) {
        this.authorities = authorities;
    }

    public static <T> AuthorityPolicyAuthorizationManager<T> hasAuthority(String authority) {
        return hasAnyAuthority(new String[]{authority});
    }

    public static <T> AuthorityPolicyAuthorizationManager<T> hasAnyRole(String prefix, String[] authorities) {
        return hasAnyAuthority(toNamedArray(prefix, authorities));
    }

    public static <T> AuthorityPolicyAuthorizationManager<T> hasAnyAuthority(String[] authorities) {
        return new AuthorityPolicyAuthorizationManager<>(Set.of(authorities));
    }

    public static String[] toNamedArray(String prefix, String[] authorities) {
        return Streamable.of(authorities).map(authority -> prefix + authority).toArray(String[]::new);
    }

    @Override
    public AccessResult check(Authentication authentication, T target) {
        return authorizationManager.check(authentication, authorities);
    }

}
