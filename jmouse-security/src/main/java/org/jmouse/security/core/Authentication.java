package org.jmouse.security.core;

import java.security.Principal;
import java.util.Collection;

/**
 * 🔐 Authentication token: principal + credentials + authorities.
 */
public interface Authentication extends Principal {

    Object getCredentials();

    Collection<? extends Authority> getAuthorities();

    boolean isAuthenticated();

    void setAuthenticated(boolean authenticated);

    Object getPrincipal();

}
