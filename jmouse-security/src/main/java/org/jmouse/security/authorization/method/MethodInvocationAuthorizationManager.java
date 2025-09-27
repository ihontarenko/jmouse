package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.AuthorizationDecision;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;

public class MethodInvocationAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    @Override
    public AuthorizationDecision check(Authentication authentication, MethodInvocation target) {
        return null;
    }

}
