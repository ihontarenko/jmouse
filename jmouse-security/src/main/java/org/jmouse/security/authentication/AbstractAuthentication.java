package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.Authority;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract public class AbstractAuthentication implements Authentication {

    protected boolean               authenticated = false;
    protected Object                principal;
    protected Collection<Authority> authorities;
    protected Object                credentials;

    public AbstractAuthentication(Object principal, Object credentials, Collection<? extends Authority> authorities) {
        this.principal = principal;
        this.authorities = List.copyOf(authorities);
        this.credentials = credentials;
        setAuthenticated(true);
    }

    public AbstractAuthentication(Object principal, Object credentials) {
        this(principal, credentials, Collections.emptyList());
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Collection<? extends Authority> getAuthorities() {
        return List.copyOf(authorities);
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    /**
     * Returns the name of this {@code Principal}.
     *
     * @return the name of this {@code Principal}.
     */
    @Override
    public String getName() {
        String username = null;

        if (this.getPrincipal() instanceof Principal principal) {
            username = principal.getName();
        }

        return username;
    }
}
