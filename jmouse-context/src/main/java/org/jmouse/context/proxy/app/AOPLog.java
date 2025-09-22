package org.jmouse.context.proxy.app;

import org.jmouse.context.proxy.aop.Intercept;
import org.jmouse.context.proxy.api.InvocationContext;
import org.jmouse.context.proxy.api.MethodInterceptor;

import java.lang.reflect.Method;

@Intercept(pointcut = "gen")
public class AOPLog implements MethodInterceptor {

    @Override
    public void after(InvocationContext context, Method method, Object[] arguments, Object result) {
        System.out.println("AOPLog after");
    }
}
