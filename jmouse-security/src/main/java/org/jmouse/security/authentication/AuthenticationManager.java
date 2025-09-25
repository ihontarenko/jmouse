package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

public interface AuthenticationManager {
    Authentication authenticate(Authentication authentication) throws AuthenticationException;
}
