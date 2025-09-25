package org.jmouse.security.authentication;

import java.util.List;

abstract public class AbstractAuthenticationManager implements AuthenticationManager {

    protected final List<AuthenticationProvider> providers;

    public AbstractAuthenticationManager(List<AuthenticationProvider> providers) {
        this.providers = List.copyOf(providers);
    }

}
