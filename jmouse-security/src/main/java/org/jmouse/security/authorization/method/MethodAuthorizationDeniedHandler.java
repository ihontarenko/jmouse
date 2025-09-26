package org.jmouse.security.authorization.method;

import org.jmouse.security.authorization.AuthorizationDecision;

import java.lang.reflect.Method;

public interface MethodAuthorizationDeniedHandler {
    Object handleDeniedInvocation(Method method, AuthorizationDecision decision);
}
