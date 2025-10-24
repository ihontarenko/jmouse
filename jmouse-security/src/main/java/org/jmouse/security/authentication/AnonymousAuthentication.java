package org.jmouse.security.authentication;

import org.jmouse.security.Authority;

import java.util.Collection;

public class AnonymousAuthentication extends AbstractAuthentication {

    public static final String ANONYMOUS = "anonymous";

    public AnonymousAuthentication(Object principal, Object credentials) {
        this(principal, credentials, Authority.ANONYMOUS);
    }

    public AnonymousAuthentication(Object principal, Object credentials, Collection<? extends Authority> authorities) {
        super(principal, credentials, authorities);
    }

    public AnonymousAuthentication(Object principal) {
        this(principal, Authority.ANONYMOUS);
    }

}
