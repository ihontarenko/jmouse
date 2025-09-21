package org.jmouse.core.proxy2.api;

import java.lang.reflect.Method;

public interface MethodInterceptor {

    default void before(InvocationContext context, Method method, Object[] arguments) {
        // no-op
    }

    default Object invoke(MethodInvocation invocation) throws Throwable {
        return invocation.proceed();
    };

    default void after(InvocationContext context, Method method, Object[] arguments, Object result) {
        // no-op
    }

}
