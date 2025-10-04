package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

public class UserIdentityAuthenticationResolver extends AbstractUserIdentityAuthenticationResolver {



    @Override
    public boolean supports(Class<?> authenticationType) {
        return false;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return null;
    }

}
