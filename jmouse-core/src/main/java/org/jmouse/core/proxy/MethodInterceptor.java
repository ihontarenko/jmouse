package org.jmouse.core.proxy;

import java.lang.reflect.Method;

/**
 * An interface defining a method interceptor, which allows code to be executed
 * before and after a method invocation. Implementations typically handle concerns
 * such as logging, security checks, or performance measurement, and then delegate
 * the actual method call to {@link #invoke(MethodInvocation)}.
 *
 * <p>Typical usage involves three steps:
 * <ul>
 *   <li>{@link #before(ProxyContext, Method, Object[])} – called before the actual method invocation.</li>
 *   <li>{@link #invoke(MethodInvocation)} – contains the parser interception logic and eventually calls
 *       {@link MethodInvocation#proceed()} to invoke the target method.</li>
 *   <li>{@link #after(ProxyContext, Method, Object[], Object)} – called after the method invocation,
 *       receiving the result of the method call (or a thrown exception, if applicable).</li>
 * </ul>
 *
 * <p>Usage binder:
 * <pre>{@code
 * public class LoggingInterceptor implements MethodInterceptor {
 *
 *     @Override
 *     public void before(ProxyContext context, Method method, Object[] arguments) {
 *         System.out.println("Before: " + method.getName());
 *     }
 *
 *     @Override
 *     public Object invoke(MethodInvocation invocation) throws Throwable {
 *         System.out.println("Invoking method: " + invocation.getMethod().getName());
 *         return invocation.proceed();
 *     }
 *
 *     @Override
 *     public void after(ProxyContext context, Method method, Object[] arguments, Object result) {
 *         System.out.println("After: " + method.getName());
 *         System.out.println("Result: " + result);
 *     }
 * }
 * }</pre>
 */
public interface MethodInterceptor {

    /**
     * Called before the target method is invoked.
     *
     * @param context   the {@link ProxyContext} containing information about the proxy
     * @param method    the method being invoked
     * @param arguments the arguments passed to the method
     */
    default void before(ProxyContext context, Method method, Object[] arguments) {
        // no-op
    }

    /**
     * The parser method that performs the actual interception logic. Implementations
     * generally call {@link MethodInvocation#proceed()} to invoke the target method.
     *
     * @param invocation the {@link MethodInvocation} encapsulating the target method and its parameters
     * @return the result of the method call
     * @throws Throwable if an error occurs during the method invocation
     */
    default Object invoke(MethodInvocation invocation) throws Throwable {
        return invocation.proceed();
    };

    /**
     * Called after the target method is invoked.
     *
     * @param context   the {@link ProxyContext} containing information about the proxy
     * @param method    the method that was invoked
     * @param arguments the arguments passed to the method
     * @param result    the result returned by the method (may be {@code null})
     */
    default void after(ProxyContext context, Method method, Object[] arguments, Object result) {
        // no-op
    }

}
