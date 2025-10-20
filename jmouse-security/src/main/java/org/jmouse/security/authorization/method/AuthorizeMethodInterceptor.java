package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.Intercept;
import org.jmouse.core.proxy.InvocationContext;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.AuthorizationManager;

import java.lang.reflect.Method;

@Intercept(Object.class)
public class AuthorizeMethodInterceptor extends AbstractAuthorizeMethodInterceptor {

    private final AuthorizationManager<MethodInvocation> authorizationManager;

    public AuthorizeMethodInterceptor(AuthorizationManager<MethodInvocation> authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public void before(InvocationContext context, Method method, Object[] arguments) {
        if (authorizationManager instanceof AuthorizeMethodManager authorizeMethodManager) {
//            authorizeMethodManager.check(null);
        }
        System.out.println(authorizationManager);
    }

    @Override
    public void after(InvocationContext context, Method method, Object[] arguments, Object result) {
        System.out.println(authorizationManager);
    }

}
