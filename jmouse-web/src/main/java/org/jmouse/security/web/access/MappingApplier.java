package org.jmouse.security.web.access;

import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.List;

@FunctionalInterface
public interface MappingApplier<T, C> {
    T apply(T owner, List<MappingMatcher> matchers, AuthorizationManager<C> manager, boolean negate);
}