package org.jmouse.testing_ground.beancontext.application.interceptors;

import org.jmouse.core.bind.bean.MethodDescriptor;
import org.jmouse.core.bind.bean.bean.JavaBeanDescriptor;
import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.proxy.annotation.ProxyMethodInterceptor;
import org.jmouse.testing_ground.beancontext.application.TestRoot;

@ProxyMethodInterceptor({TestRoot.class})
public class OloloInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        MethodDescriptor descriptor = MethodDescriptor.forMethod(invocation.getMethod());

        System.out.println(descriptor);

        Object returnValue = invocation.proceed();

        JavaBeanDescriptor<Object> beanDescriptor = JavaBeanDescriptor.forBean((Class<Object>) returnValue.getClass(), Object.class);

        System.out.println(beanDescriptor);

        if (beanDescriptor.isNumber()) {
            throw new IllegalStateException("Method '%s' disallowed return a number".formatted(descriptor));
        }

        return returnValue;
    }

}
