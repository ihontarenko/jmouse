package org.jmouse.security.authentication;

import org.jmouse.security.core.Authority;

public class AnonymousAuthentication extends AbstractAuthentication {

    public AnonymousAuthentication() {
        super(Authority.ANONYMOUS);
    }

}
