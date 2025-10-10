package org.jmouse.security.authorization.method;

import org.jmouse.security.authorization.AuthorizationException;
import org.jmouse.security.authorization.AccessResult;

import java.lang.reflect.Method;

public final class ThrowingMethodAuthorizationDeniedHandler implements MethodAuthorizationDeniedHandler {
    @Override
    public Object handleDeniedInvocation(Method method, AccessResult decision) {
        throw new AuthorizationException("Denied " + method + " : " + decision.getMessage());
    }
}