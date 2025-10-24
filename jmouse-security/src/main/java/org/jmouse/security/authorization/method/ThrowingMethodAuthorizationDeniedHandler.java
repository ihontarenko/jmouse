package org.jmouse.security.authorization.method;

import org.jmouse.core.reflection.Reflections;
import org.jmouse.security.authorization.AccessResult;

public final class ThrowingMethodAuthorizationDeniedHandler implements MethodAuthorizationDeniedHandler {
    @Override
    public Object handleDeniedInvocation(MethodAuthorizationContext invocation, AccessResult decision) {
        throw new MethodAuthorizationException(
                "Access denied! Unauthorized method invocation. "
                        + Reflections.getMethodName(invocation.proxyInvocation().getMethod()), invocation);
    }
}