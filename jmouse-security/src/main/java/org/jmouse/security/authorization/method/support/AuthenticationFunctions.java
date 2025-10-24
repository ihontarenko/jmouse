package org.jmouse.security.authorization.method.support;

import org.jmouse.security.authentication.AuthenticationInspector;
import org.jmouse.security.authentication.AuthenticationLevel;
import org.jmouse.security.Authentication;

public class AuthenticationFunctions implements AuthenticationInspector {

    public static final String NAMESPACE = "a";

    private final Authentication authentication;

    public AuthenticationFunctions(Authentication authentication) {
        this.authentication = authentication;
    }

    public boolean isAuthenticated() {
        return authentication.isAuthenticated();
    }

    public boolean isAnonymous() {
        return isAnonymous(authentication);
    }

    public boolean isPartiallyAuthenticated() {
        return isPartiallyAuthenticated(authentication);
    }

    public boolean isFullyAuthenticated() {
        return isFullyAuthenticated(authentication);
    }

    public AuthenticationLevel level() {
        return level(authentication);
    }
}
