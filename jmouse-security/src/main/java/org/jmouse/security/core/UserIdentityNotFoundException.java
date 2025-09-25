package org.jmouse.security.core;

public class UserIdentityNotFoundException extends RuntimeException {

    public UserIdentityNotFoundException(String username) {
        super("USER IDENTITY NOT FOUND: " + username);
    }

}