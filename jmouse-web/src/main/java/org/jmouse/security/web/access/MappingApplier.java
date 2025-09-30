package org.jmouse.security.web.access;

import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.web.RequestMatcher;

import java.util.List;

@FunctionalInterface
public interface MappingApplier<T, C> {
    T apply(T owner, List<RequestMatcher> matchers, AuthorizationManager<C> manager, boolean negate);
}