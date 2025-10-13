package org.jmouse.security.core;

import org.jmouse.security.authentication.AuthenticationException;

public class BadCredentialsException extends AuthenticationException {

    public BadCredentialsException(String message) {
        super(message);
    }

    public BadCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

}
