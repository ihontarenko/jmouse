package org.jmouse.security;

public class PasswordMismatchException extends BadCredentialsException {

    public PasswordMismatchException(String username) {
        super("PASSWORD MISMATCH: " + username);
    }

}