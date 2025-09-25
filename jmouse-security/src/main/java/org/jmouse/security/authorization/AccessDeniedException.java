package org.jmouse.security.authorization;

/**
 * ⛔ Thrown when an authenticated principal lacks required access rights.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

}