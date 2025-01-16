package test.application.interceptors;

import svit.proxy.MethodInterceptor;
import svit.proxy.MethodInvocation;
import svit.proxy.annotation.ProxyMethodInterceptor;
import test.application.TestRoot;
import test.application.Utils;

@ProxyMethodInterceptor({TestRoot.class})
public class OloloInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println(invocation);
        return invocation.proceed();
    }

}
