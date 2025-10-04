package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

/**
 * 🛂 Checks a specific type of credentials and returns Authentication if successful.
 */
public interface AuthenticationResolver {

    boolean supports(Class<?> authenticationType);

    Authentication authenticate(Authentication authentication) throws AuthenticationException;

}
