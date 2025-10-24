package org.jmouse.security.authorization;

import org.jmouse.security.Authentication;

/**
 * 🚦 Decides access for a given secured target (endpoint/method/message).
 */
@FunctionalInterface
public interface AuthorizationManager<T> {
    AccessResult check(Authentication authentication, T target);
}
