package org.jmouse.security.web.access;

import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.EnrichedRequestContext;

import java.util.function.BiFunction;

public final class AuthorizationPolicies {

    private AuthorizationPolicies() {
    }

    public static AuthorizationManager<EnrichedRequestContext> permitAll() {
        return (authentication, target) -> AccessResult.PERMIT;
    }

    public static AuthorizationManager<EnrichedRequestContext> denyAll() {
        return (authentication, target) -> AccessResult.DENY;
    }

    public static <T> AuthorizationManager<T> not(AuthorizationManager<T> delegate) {
        return (Authentication authentication, T t) -> delegate.check(authentication, t)
                .isGranted() ? AccessResult.DENY : AccessResult.PERMIT;
    }

    public static <T> AuthorizationManager<T> authenticated() {
        return (authentication, c) -> (authentication != null && authentication.isAuthenticated())
                ? AccessResult.PERMIT : AccessResult.DENY;
    }

    public static AuthorizationManager<EnrichedRequestContext> of(
            BiFunction<Authentication, EnrichedRequestContext, AccessResult> function) {
        return function::apply;
    }
}
