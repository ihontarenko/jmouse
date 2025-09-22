package org.jmouse.core.proxy.old;

import org.jmouse.core.proxy.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import static java.lang.reflect.Proxy.*;

/**
 * A proxy implementation using the Java Dynamic ProxyProvider API.
 * <p>
 * This class acts as an {@link InvocationHandler} for method calls on proxied objects.
 * It supports method interception through a chain of {@link MethodInterceptor}s and
 * ensures proper handling of special methods like {@code equals}, {@code hashCode}, and {@code toString}.
 */
public class JdkProxy implements InvocationHandler, ProxyProvider, ProxyIntrospection {

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
        return ProxyInvoke.invokeCore(JdkProxyEngine.ENGINE_NAME, proxyContext, proxy, method, arguments);
    }

    /**
     * Creates and returns a proxy instance for the target structured using the specified {@link ClassLoader}.
     *
     * @return the proxy instance.
     */
    @Override
    public Object getProxy() {
        return newProxyInstance(proxyContext.getClassLoader(),
                                proxyContext.getInterfaces().toArray(Class[]::new), this);
    }

    @Override
    public ProxyDefinition<?> getProxyDefinition() {
        return proxyContext;
    }

    /**
     * Computes the hash code for this proxy based on the target structured and the proxy configuration.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return 31 * JdkProxy.class.hashCode() + proxyContext.getTarget().hashCode();
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
        }

        ProxyContext context = ProxyIntrospection.tryExtractContext(that);

        if (context == null) {
            return false;
        }

        Class<?>[] a = proxyContext.getInterfaces().toArray(Class[]::new);
        Class<?>[] b = context.getInterfaces().toArray(Class[]::new);

        return Arrays.equals(a, b) && proxyContext.getTarget().equals(context.getTarget());
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
