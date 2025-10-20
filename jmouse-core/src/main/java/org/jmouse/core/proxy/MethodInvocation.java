package org.jmouse.core.proxy;

import org.jmouse.core.reflection.Executables;
import org.jmouse.util.Arrays;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.Arrays.stream;

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

    void setArgumentsUnsafe(Object[] arguments);

    default void setArgument(int index, Object newValue) {
        setArgumentsUnsafe(Executables.updateArgument(getMethod(), getArguments(), index, newValue));
    }

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

    /**
     * 🎯 Retrieves the return value produced by the invocation.
     *
     * <p>By default, always returns {@code null}.
     * Concrete implementations may override this to provide
     * the actual method result.</p>
     *
     * @return the method return value, or {@code null} if not set
     */
    default Object getReturnValue() {
        return null;
    }

    /**
     * 📝 Sets the return value for this invocation.
     *
     * <p>Default implementation is a NO-OP. Override in order
     * to allow interceptors or frameworks to change the result
     * of the target method.</p>
     *
     * @param returnValue the new return value (may be {@code null})
     */
    default void setReturnValue(Object returnValue) {
        // NO-OP
    }

    default Class<?> getReturnType() {
        return getMethod().getReturnType();
    }

    @SuppressWarnings("unchecked")
    default <T> Optional<T> getTypedArgument(Class<T> type) {
        return (Optional<T>) stream(getArguments())
                .filter(argument -> argument.getClass().isInstance(argument)).findFirst();
    }

    default boolean isVoidMethod() {
        return getReturnType() == void.class || getReturnType() == Void.class;
    }

}
