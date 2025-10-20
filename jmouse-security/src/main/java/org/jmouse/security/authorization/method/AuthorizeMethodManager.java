package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;

public class AuthorizeMethodManager implements AuthorizationManager<MethodInvocation> {

    private AuthorizeExpressionAttributeRegistry attributeRegistry = new AuthorizeExpressionAttributeRegistry(
            new SecurityMethodExpressionHandler()
    );

    @Override
    public AccessResult check(Authentication authentication, MethodInvocation target) {
        return null;
    }

    public AuthorizeExpressionAttributeRegistry getAttributeRegistry() {
        return attributeRegistry;
    }

    public void setAttributeRegistry(AuthorizeExpressionAttributeRegistry attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
    }
}
