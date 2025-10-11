package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

abstract public class AbstractUsernamePasswordAuthenticationResolver extends AbstractAuthenticationResolver {

    @Override
    protected Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        return null;
    }

    @Override
    public boolean supports(Class<?> authenticationType) {
        return UsernamePasswordAuthentication.class.isAssignableFrom(authenticationType);
    }

}
