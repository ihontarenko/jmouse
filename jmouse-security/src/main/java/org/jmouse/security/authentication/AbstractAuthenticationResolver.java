package org.jmouse.security.authentication;

import org.jmouse.security.Authentication;

abstract public class AbstractAuthenticationResolver implements AuthenticationResolver {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication after = doAuthenticate(authentication);

        return after;
    }

    abstract protected Authentication doAuthenticate(Authentication authentication) throws AuthenticationException;

}
