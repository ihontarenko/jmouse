package org.jmouse.security;

import java.security.Principal;
import java.util.Collection;
import org.jmouse.security.authentication.AuthenticationLevel;

/**
 * 🔐 Authentication token: principal + credentials + authorities.
 */
public interface Authentication extends Principal {
    Object getCredentials();

    Collection<? extends Authority> getAuthorities();

    boolean isAuthenticated();

    void setAuthenticated(boolean authenticated);

    Object getPrincipal();

    default AuthenticationLevel level() {
        return isAuthenticated() ? AuthenticationLevel.FULL : AuthenticationLevel.ANONYMOUS;
    }
}
