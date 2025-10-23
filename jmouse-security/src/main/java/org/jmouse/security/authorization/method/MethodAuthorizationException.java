package org.jmouse.security.authorization.method;

import org.jmouse.security.authorization.AuthorizationException;

public class MethodAuthorizationException extends AuthorizationException {

    private final MethodAuthorizationContext invocation;

    public MethodAuthorizationException(String message, MethodAuthorizationContext invocation) {
        super(message);
        this.invocation = invocation;
    }

    public MethodAuthorizationException(String message, Throwable cause, MethodAuthorizationContext invocation) {
        super(message, cause);
        this.invocation = invocation;
    }

}
