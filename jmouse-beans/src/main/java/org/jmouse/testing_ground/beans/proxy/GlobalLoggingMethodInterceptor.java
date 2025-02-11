package org.jmouse.testing_ground.beans.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.proxy.MethodInvocationDecorator;
import org.jmouse.core.proxy.ProxyContext;
import org.jmouse.core.proxy.annotation.ProxyMethodInterceptor;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;

/**
 * A global logging interceptor that logs method calls before and after their invocation,
 * as well as during the invocation process. This interceptor is annotated with
 * {@link ProxyMethodInterceptor @ProxyMethodInterceptor} to indicate that it can be
 * automatically discovered and applied by a compatible proxy factory.
 *
 * @see MethodInterceptor
 * @see ProxyMethodInterceptor
 * @see MethodInvocation
 */
@ProxyMethodInterceptor({Object.class})
public class GlobalLoggingMethodInterceptor implements MethodInterceptor {

    private static final Logger LOGGER    = LoggerFactory.getLogger(GlobalLoggingMethodInterceptor.class);
    private static final String SEPARATOR = "-----------------------";

    /**
     * Called before the target method is invoked. Logs the method name at {@code INFO} level.
     *
     * @param context   the {@link ProxyContext} containing information about the proxy
     * @param method    the method being invoked
     * @param arguments the arguments passed to the method
     */
    @Override
    public void before(ProxyContext context, Method method, Object[] arguments) {
        LOGGER.info(SEPARATOR);
        LOGGER.info("Before: {}", Reflections.getMethodName(method));
    }

    /**
     * The core interception logic, which logs the method invocation using a
     * {@link MethodInvocationDecorator} and then proceeds with the original method call.
     *
     * @param invocation the {@link MethodInvocation} encapsulating the target method
     *                   and its parameters
     * @return the result of the method call
     * @throws Throwable if an error occurs during method invocation
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        MethodInvocationDecorator decorator = new MethodInvocationDecorator(invocation);

        LOGGER.info(SEPARATOR);
        LOGGER.info("Invocation: {}", Reflections.getMethodName(decorator.getMethod()));

        return invocation.proceed();
    }

    /**
     * Called after the target method is invoked. Logs the method name and the result at
     * {@code INFO} level.
     *
     * @param context   the {@link ProxyContext} containing information about the proxy
     * @param method    the method that was invoked
     * @param arguments the arguments passed to the method
     * @param result    the result returned by the method (may be {@code null})
     */
    @Override
    public void after(ProxyContext context, Method method, Object[] arguments, Object result) {
        LOGGER.info(SEPARATOR);
        LOGGER.info("After: {}", Reflections.getMethodName(method));
        LOGGER.info("Result: {}", result);
        LOGGER.info(SEPARATOR);
    }
}
