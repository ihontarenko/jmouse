package org.jmouse.testing_ground.beancontext.application.interceptors;

import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.proxy.annotation.ProxyMethodInterceptor;
import org.jmouse.testing_ground.beancontext.application.TestRoot;

@ProxyMethodInterceptor({TestRoot.class})
public class OloloInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println(invocation);
        return invocation.proceed();
    }

}
