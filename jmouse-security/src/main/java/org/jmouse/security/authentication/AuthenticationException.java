package org.jmouse.security.authentication;

/**
 * ❌ Thrown when authentication fails or is required.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

}