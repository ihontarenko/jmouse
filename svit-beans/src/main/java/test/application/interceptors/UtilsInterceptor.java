package test.application.interceptors;

import svit.proxy.MethodInterceptor;
import svit.proxy.MethodInvocation;
import svit.proxy.annotation.ProxyMethodInterceptor;
import test.application.Utils;

@ProxyMethodInterceptor({Utils.class})
public class UtilsInterceptor implements MethodInterceptor {

    /**
     * Intercepts a method invocation on a proxy instance.
     * <p>
     * Implementations can add behavior before or after the invocation,
     * or even replace the method execution entirely.
     * </p>
     *
     * @param invocation the {@link MethodInvocation} object containing details of the method call.
     * @return the result of the method invocation.
     * @throws Throwable if an error occurs during method interception or execution.
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        if (invocation.getMethod().getName().equals("setBeanContext")) {
            System.out.println(invocation.getArguments()[0]);
        }

        return invocation.proceed();
    }

}
