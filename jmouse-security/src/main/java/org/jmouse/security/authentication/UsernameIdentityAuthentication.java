package org.jmouse.security.authentication;

import org.jmouse.security.core.Authority;

import java.util.Collection;

public class UsernameIdentityAuthentication extends AbstractAuthentication {

    public UsernameIdentityAuthentication(Collection<Authority> authorities) {
        super(authorities);
    }

}
