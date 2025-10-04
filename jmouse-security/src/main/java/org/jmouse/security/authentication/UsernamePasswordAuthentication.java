package org.jmouse.security.authentication;

import org.jmouse.security.core.Authority;

import java.util.Collection;

public class UsernamePasswordAuthentication extends AbstractAuthentication {

    public UsernamePasswordAuthentication(Object principal, Object credentials, Collection<? extends Authority> authorities) {
        super(principal, credentials, authorities);
    }

    public UsernamePasswordAuthentication(Object principal, Object credentials) {
        super(principal, credentials);
    }

}
