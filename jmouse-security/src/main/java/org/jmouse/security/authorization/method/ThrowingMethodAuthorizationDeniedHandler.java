package org.jmouse.security.authorization.method;

import org.jmouse.security.authorization.AccessResult;

public final class ThrowingMethodAuthorizationDeniedHandler implements MethodAuthorizationDeniedHandler {
    @Override
    public Object handleDeniedInvocation(MethodAuthorizationContext invocation, AccessResult decision) {
        throw new MethodAuthorizationException(
                "Denied " + invocation.proxyInvocation().getMethod() + " : " + decision.getMessage(), invocation);
    }
}