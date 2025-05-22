package org.jmouse.core.proxy.jdk;

import org.jmouse.core.proxy.*;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static java.lang.reflect.Proxy.*;

/**
 * A proxy implementation using the Java Dynamic Proxy API.
 * <p>
 * This class acts as an {@link InvocationHandler} for method calls on proxied objects.
 * It supports method interception through a chain of {@link MethodInterceptor}s and
 * ensures proper handling of special methods like {@code equals}, {@code hashCode}, and {@code toString}.
 */
public class JdkProxy implements InvocationHandler, Proxy {

    private final ProxyContext proxyContext;

    /**
     * Constructs a new {@code JdkProxy} with the given {@link ProxyContext}.
     *
     * @param proxyContext the configuration for the proxy, including the target structured and interceptors.
     */
    public JdkProxy(ProxyContext proxyContext) {
        this.proxyContext = proxyContext;
    }

    /**
     * Handles method invocations on the proxy instance.
     *
     * @param proxy     the proxy instance.
     * @param method    the method being invoked.
     * @param arguments the arguments passed to the method.
     * @return the result of the method invocation.
     * @throws Throwable if an error occurs during method execution.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        Object   returnValue;
        Class<?> returnClass = method.getReturnType();
        Object   target      = proxyContext.getTarget();

        try {
            if (Reflections.isEqualsMethod(method) && !proxyContext.hasEquals()) {
                // if target does not have own 'equals' method
                return this.equals(arguments[0]);
            } else if (Reflections.isHashCodeMethod(method) && !proxyContext.hasHashCode()) {
                // if target does not have own 'hashCode' method
                return this.hashCode();
            }

            List<MethodInterceptor> interceptors = proxyContext.getInterceptors();
            MethodInvocation invocation   = new MethodInvocationChain(
                    proxy, target, method, arguments, interceptors, proxyContext);

            returnValue = invocation.proceed();
        } catch (Throwable throwable) {
            throw new ProxyInvocationException(throwable.getMessage(), throwable);
        }

        if (returnValue != null && returnValue == target && returnClass != Object.class && returnClass.isInstance(
                proxy)) {
            returnValue = proxy;
        } else if (returnValue == null && returnClass.isPrimitive() && returnClass != void.class) {
            throw new ProxyInvocationException(
                    "Method '%s' returned null, but a primitive '%s' value was expected."
                            .formatted(method, returnClass));
        }

        return returnValue;
    }

    /**
     * Creates and returns a proxy instance for the target structured using the specified {@link ClassLoader}.
     *
     * @return the proxy instance.
     */
    @Override
    public Object getProxy() {
        return newProxyInstance(proxyContext.getClassLoader(), proxyContext.getInterfaces().toArray(Class[]::new), this);
    }

    /**
     * Computes the hash code for this proxy based on the target structured and the proxy configuration.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return JdkProxy.class.hashCode() * 13 + proxyContext.getTarget().hashCode();
    }

    /**
     * Compares this proxy with another structured for equality.
     *
     * @param that the structured to compare.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        } else if (that == null) {
            return false;
        }

        JdkProxy proxy;

        if (that instanceof JdkProxy jdkProxy) {
            proxy = jdkProxy;
        } else if (isProxyClass(that.getClass())) {
            InvocationHandler invocationHandler = getInvocationHandler(that);
            if (!(invocationHandler instanceof JdkProxy jdkProxy)) {
                return false;
            }
            proxy = jdkProxy;
        } else {
            return false;
        }

        Class<?>[] interfacesA = proxy.proxyContext.getInterfaces().toArray(Class[]::new);
        Class<?>[] interfacesB = proxyContext.getInterfaces().toArray(Class[]::new);

        return Arrays.equals(interfacesA, interfacesB)
                && proxy.proxyContext.getTarget().equals(proxyContext.getTarget());
    }

    /**
     * Returns a string representation of this proxy.
     *
     * @return a string containing the proxy's hash code and configuration details.
     */
    @Override
    public String toString() {
        return "JDK PROXY [%s]".formatted(proxyContext.getTargetClass());
    }
}
