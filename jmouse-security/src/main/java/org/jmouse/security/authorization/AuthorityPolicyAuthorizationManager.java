package org.jmouse.security.authorization;

import org.jmouse.core.Streamable;
import org.jmouse.security.authentication.AnonymousAuthentication;
import org.jmouse.security.core.Authentication;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;

public final class AuthorityPolicyAuthorizationManager<T> implements AuthorizationManager<T> {

    public static final String ROLE_PREFIX = "ROLE_";

    private final AuthoritiesAuthorizationManager authorizationManager = new AuthoritiesAuthorizationManager();
    private final Collection<String>              authorities;

    public AuthorityPolicyAuthorizationManager(Collection<String> authorities) {
        this.authorities = authorities;
    }

    public static <T> AuthorizationManager<T> permitAll() {
        return (authentication, target) -> AccessResult.PERMIT;
    }

    public static <T> AuthorizationManager<T> denyAll() {
        return (authentication, target) -> AccessResult.DENY;
    }

    public static <T> AuthorizationManager<T> not(AuthorizationManager<T> delegate) {
        return (Authentication authentication, T t) -> delegate.check(authentication, t)
                .isGranted() ? AccessResult.DENY : AccessResult.PERMIT;
    }

    public static <T> AuthorizationManager<T> anonymous() {
        return (authentication, c) -> (authentication instanceof AnonymousAuthentication)
                ? AccessResult.PERMIT : AccessResult.DENY;
    }

    public static <T> AuthorizationManager<T> authenticated() {
        return (authentication, c) -> (authentication != null && authentication.isAuthenticated())
                ? AccessResult.PERMIT : AccessResult.DENY;
    }

    public static <T> AuthorizationManager<T> of(
            BiFunction<Authentication, T, AccessResult> function) {
        return function::apply;
    }

    public static <T> AuthorityPolicyAuthorizationManager<T> hasRole(String prefix, String role) {
        return hasAnyRole(prefix, new String[]{role});
    }

    public static <T> AuthorityPolicyAuthorizationManager<T> hasRole(String role) {
        return hasRole(ROLE_PREFIX, role);
    }

    public static <T> AuthorityPolicyAuthorizationManager<T> hasAuthority(String authority) {
        return hasAnyAuthority(new String[]{authority});
    }

    public static <T> AuthorityPolicyAuthorizationManager<T> hasAnyRole(String[] roles) {
        return hasAnyRole(ROLE_PREFIX, roles);
    }

    public static <T> AuthorityPolicyAuthorizationManager<T> hasAnyRole(String prefix, String[] roles) {
        return hasAnyAuthority(toNamedArray(prefix, roles));
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
