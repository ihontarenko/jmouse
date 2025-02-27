package org.jmouse.testing_ground.beancontext.application.interceptors;

import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.proxy.annotation.ProxyMethodInterceptor;
import org.jmouse.testing_ground.beancontext.application.Utils;

@ProxyMethodInterceptor({Utils.class})
public class UtilsInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return invocation.proceed();
    }

}
