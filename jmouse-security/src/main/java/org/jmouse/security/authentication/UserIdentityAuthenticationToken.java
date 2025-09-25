package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

public class UserIdentityAuthenticationToken extends AbstractUserIdentityAuthenticationProvider {

    @Override
    public boolean supports(Class<?> authenticationType) {
        return false;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return null;
    }

}
