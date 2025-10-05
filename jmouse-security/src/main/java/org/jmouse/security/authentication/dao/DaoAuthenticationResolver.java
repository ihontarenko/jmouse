package org.jmouse.security.authentication.dao;

import org.jmouse.security.authentication.AbstractUserIdentityAuthenticationResolver;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.core.Authentication;

public class DaoAuthenticationResolver extends AbstractUserIdentityAuthenticationResolver {
    @Override
    public boolean supports(Class<?> authenticationType) {
        return UsernamePasswordAuthentication.class.isAssignableFrom(authenticationType);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return null;
    }
}
