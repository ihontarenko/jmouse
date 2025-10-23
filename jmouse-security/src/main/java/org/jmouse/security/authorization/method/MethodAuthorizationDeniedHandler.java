package org.jmouse.security.authorization.method;

import org.jmouse.security.authorization.AccessResult;

public interface MethodAuthorizationDeniedHandler {
    Object handleDeniedInvocation(MethodAuthorizationContext invocation, AccessResult decision);
}
