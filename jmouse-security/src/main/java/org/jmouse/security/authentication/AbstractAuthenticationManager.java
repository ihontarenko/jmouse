package org.jmouse.security.authentication;

import org.jmouse.security.Authentication;

import java.util.ArrayList;
import java.util.List;

abstract public class AbstractAuthenticationManager implements AuthenticationManager {

    protected final List<AuthenticationResolver> resolvers = new ArrayList<>();

    public AbstractAuthenticationManager(List<AuthenticationResolver> resolvers) {
        this.resolvers.addAll(resolvers);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return doAuthenticate(authentication);
    }

    abstract protected Authentication doAuthenticate(Authentication authentication);

}
