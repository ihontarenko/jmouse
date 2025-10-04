package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

import java.util.List;

abstract public class AbstractAuthenticationManager implements AuthenticationManager {

    protected final List<AuthenticationResolver> resolvers;

    public AbstractAuthenticationManager(List<AuthenticationResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return doAuthenticate(authentication);
    }

    abstract protected Authentication doAuthenticate(Authentication authentication);

}
