package org.jmouse.security.authorization;

import org.jmouse.security.core.Authentication;

/**
 * 🚦 Decides access for a given secured target (endpoint/method/message).
 */
@FunctionalInterface
public interface AuthorizationManager<T> {
    AuthorizationDecision check(Authentication authentication, T target);
}
