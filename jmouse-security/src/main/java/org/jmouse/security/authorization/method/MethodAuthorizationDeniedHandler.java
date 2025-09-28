package org.jmouse.security.authorization.method;

import org.jmouse.security.authorization.AccessResult;

import java.lang.reflect.Method;

public interface MethodAuthorizationDeniedHandler {
    Object handleDeniedInvocation(Method method, AccessResult decision);
}
