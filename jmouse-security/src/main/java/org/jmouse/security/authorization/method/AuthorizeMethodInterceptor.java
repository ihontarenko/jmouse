package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.Intercept;
import org.jmouse.core.proxy.InvocationContext;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.SecurityContextHolderStrategy;

import java.lang.reflect.Method;
import java.util.function.Supplier;

@Intercept(Object.class)
public class AuthorizeMethodInterceptor extends AbstractAuthorizeMethodInterceptor {

    private final AuthorizationManager<MethodInvocation>  authorizationManager;
    private final Supplier<SecurityContextHolderStrategy> contextHolder;

    {
        contextHolder = SecurityContextHolder::getContextHolderStrategy;
    }

    public AuthorizeMethodInterceptor(AuthorizationManager<MethodInvocation> authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (authorizationManager instanceof AuthorizeMethodManager authorizeMethodManager) {
            authorizeMethodManager.check(getAuthentication(), invocation);
        }

        Object result = invocation.proceed();

        if (authorizationManager instanceof AuthorizeMethodManager authorizeMethodManager) {
            authorizeMethodManager.check(getAuthentication(), invocation);
        }

        return result;
    }

    private Authentication getAuthentication() {
        SecurityContextHolderStrategy contextHolderStrategy = contextHolder.get();
        Authentication authentication = contextHolderStrategy.getContext().getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("An authentication is not present in context.");
        }

        return authentication;
    }

}
