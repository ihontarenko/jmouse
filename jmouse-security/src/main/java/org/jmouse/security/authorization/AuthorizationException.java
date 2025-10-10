package org.jmouse.security.authorization;

/**
 * ⛔ Thrown when an authenticated principal lacks required access rights.
 */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

}