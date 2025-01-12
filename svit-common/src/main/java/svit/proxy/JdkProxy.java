package svit.proxy;

import svit.reflection.Reflections;

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

    private final ProxyConfig proxyConfig;

    /**
     * Constructs a new {@code JdkProxy} with the given {@link ProxyConfig}.
     *
     * @param proxyConfig the configuration for the proxy, including the target object and interceptors.
     */
    public JdkProxy(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
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
        Object   target      = proxyConfig.getTarget();

        try {
            if (Reflections.isEqualsMethod(method) && !proxyConfig.hasEquals()) {
                // if target does not have own 'equals' method
                return this.equals(arguments[0]);
            } else if (Reflections.isHashCodeMethod(method) && !proxyConfig.hasHashCode()) {
                // if target does not have own 'hashCode' method
                return this.hashCode();
            }

            List<MethodInterceptor> interceptors = proxyConfig.getInterceptors();
            MethodInvocation        invocation   = new MethodInvocationChain(
                    proxy, target, method, arguments, interceptors, proxyConfig);

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
     * Creates and returns a proxy instance for the target object using the specified {@link ClassLoader}.
     *
     * @param classLoader the class loader to define the proxy class.
     * @return the proxy instance.
     */
    @Override
    public Object getProxy(ClassLoader classLoader) {
        return newProxyInstance(classLoader, proxyConfig.getInterfaces().toArray(Class[]::new), this);
    }

    /**
     * Returns the {@link ProxyEngine} associated with this proxy implementation.
     *
     * @return {@link ProxyEngine#JDK}.
     */
    @Override
    public ProxyEngine getProxyEngine() {
        return ProxyEngine.JDK;
    }

    /**
     * Computes the hash code for this proxy based on the target object and the proxy configuration.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return JdkProxy.class.hashCode() * 13 + proxyConfig.getTarget().hashCode();
    }

    /**
     * Compares this proxy with another object for equality.
     *
     * @param that the object to compare.
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

        Class<?>[] interfacesA = proxy.proxyConfig.getInterfaces().toArray(Class[]::new);
        Class<?>[] interfacesB = proxyConfig.getInterfaces().toArray(Class[]::new);

        return Arrays.equals(interfacesA, interfacesB)
                && proxy.proxyConfig.getTarget().equals(proxyConfig.getTarget());
    }

    /**
     * Returns a string representation of this proxy.
     *
     * @return a string containing the proxy's hash code and configuration details.
     */
    @Override
    public String toString() {
        return "JDK PROXY [%s] [%s]".formatted(hashCode(), super.toString());
    }
}
