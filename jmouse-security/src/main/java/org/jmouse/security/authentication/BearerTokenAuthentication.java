package org.jmouse.security.authentication;

import org.jmouse.security.Authority;

import java.util.Collection;

public class BearerTokenAuthentication extends AbstractAuthentication {

    public BearerTokenAuthentication(Object principal, Object credentials, Collection<? extends Authority> authorities) {
        super(principal, credentials, authorities);
    }

    public BearerTokenAuthentication(Object principal, Object credentials) {
        super(principal, credentials);
    }

}
