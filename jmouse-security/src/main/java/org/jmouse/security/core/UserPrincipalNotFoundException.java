package org.jmouse.security.core;

import org.jmouse.security.authentication.AuthenticationException;

public class UserPrincipalNotFoundException extends AuthenticationException {

    public UserPrincipalNotFoundException(String username) {
        super("USER-PRINCIPAL NOT FOUND: " + username);
    }

}