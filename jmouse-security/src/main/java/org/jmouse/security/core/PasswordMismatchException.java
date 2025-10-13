package org.jmouse.security.core;

public class PasswordMismatchException extends BadCredentialsException {

    public PasswordMismatchException(String username) {
        super("PASSWORD MISMATCH: " + username);
    }

}