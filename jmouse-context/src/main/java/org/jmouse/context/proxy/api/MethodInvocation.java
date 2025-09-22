package org.jmouse.context.proxy.api;

import java.lang.reflect.Method;

/**
 * Represents a method invocation on a proxy instance, allowing for method interception and custom behavior.
 * <p>
 * The {@link MethodInvocation} interface provides access to the method being called, its arguments, the target structured,
 * and other contextual information. It also supports proceeding with the original method invocation or altering its behavior.
 * </p>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * MethodInvocation invocation = ...;
 * System.out.println("Invoking method: " + invocation.getMethod().getName());
 * Object result = invocation.proceed();
 * System.out.println("Method result: " + result);
 * }</pre>
 */
public interface MethodInvocation {

    /**
     * Proceeds with the method invocation, executing the next interceptor in the chain or the original method.
     *
     * @return the result of the method invocation.
     * @throws Throwable if an error occurs during method execution.
     */
    Object proceed() throws Throwable;

    /**
     * Retrieves the method being invoked.
     *
     * @return the {@link Method} structured representing the invoked method.
     */
    Method getMethod();

    /**
     * Retrieves the arguments passed to the method.
     *
     * @return an array of {@link Object} representing the method arguments.
     */
    Object[] getArguments();

    /**
     * Retrieves the target structured on which the method is being invoked.
     *
     * @return the target structured.
     */
    Object getTarget();

    /**
     * Retrieves the ordinal position of this invocation in the interceptor chain.
     *
     * @return the ordinal index of this invocation.
     */
    int getOrdinal();

    /**
     * Retrieves the proxy instance on which the method is being invoked.
     *
     * @return the proxy instance.
     */
    Object getProxy();
}
