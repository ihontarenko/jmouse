package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;

public class MethodInvocationAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    @Override
    public AccessResult check(Authentication authentication, MethodInvocation target) {
        return null;
    }

}
