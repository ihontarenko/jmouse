package org.jmouse.security.authorization;

import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.core.Authentication;

/**
 * 🛂 Checks a specific type of credentials and returns Authentication if successful.
 */
public interface AuthenticationProvider {

    boolean supports(Class<?> authenticationType);

    Authentication authenticate(Authentication authentication) throws AuthenticationException;

}
