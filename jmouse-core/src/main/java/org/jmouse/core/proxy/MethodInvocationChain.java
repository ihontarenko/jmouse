package org.jmouse.core.proxy;

import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.List;


/**
 * Represents a chain of method interceptors for a proxy method invocation.
 * <p>
 * The {@code MethodInvocationChain} executes registered {@link MethodInterceptor}s sequentially,
 * with the final step invoking the actual method on the target object.
 */
public class MethodInvocationChain implements MethodInvocation {

    protected final List<MethodInterceptor> interceptors;
    protected final Object                  target;
    protected final Method                  method;
    protected final Object[]                arguments;
    protected final Object                  proxy;
    protected final ProxyContext            proxyContext;
    protected       int                     currentIndex = -1;

    /**
     * Constructs a new {@code MethodInvocationChain}.
     *
     * @param proxy        the proxy instance.
     * @param target       the target object.
     * @param method       the method being invoked.
     * @param arguments    the arguments passed to the method.
     * @param interceptors the list of method interceptors.
     * @param proxyContext  the proxy configuration.
     */
    public MethodInvocationChain(Object proxy, Object target, Method method, Object[] arguments,
                                 List<MethodInterceptor> interceptors, ProxyContext proxyContext) {
        this.interceptors = interceptors;
        this.proxyContext = proxyContext;
        this.proxy = proxy;
        this.target = target;
        this.method = method;
        this.arguments = arguments;
    }

    /**
     * Proceeds to the next interceptor in the chain or invokes the actual method on the target object.
     *
     * @return the result of the method invocation.
     * @throws Throwable if an error occurs during method execution.
     */
    @Override
    public Object proceed() throws Throwable {
        MethodInterceptor interceptor;

        // shift and execute next interceptor in the chain
        if (interceptors.size() > ++currentIndex) {
            interceptor = interceptors.get(currentIndex);

            interceptor.before(getProxyContext(), getMethod(), getArguments());
            Object result = interceptor.invoke(this);
            interceptor.after(getProxyContext(), getMethod(), getArguments(), result);

            return result;
        }

        // invoke real method from target object in the end of chain
        return Reflections.invokeMethod(target, method, arguments);
    }

    /**
     * Returns the method being invoked.
     *
     * @return the {@link Method}.
     */
    @Override
    public Method getMethod() {
        return method;
    }

    /**
     * Returns the arguments passed to the method.
     *
     * @return an array of method arguments.
     */
    @Override
    public Object[] getArguments() {
        return arguments;
    }

    /**
     * Returns the target object on which the method is invoked.
     *
     * @return the target object.
     */
    @Override
    public Object getTarget() {
        return target;
    }

    /**
     * Returns the current index of the interceptor in the chain.
     *
     * @return the index of the current interceptor.
     */
    @Override
    public int getOrdinal() {
        return currentIndex;
    }

    /**
     * Returns the proxy configuration associated with this invocation.
     *
     * @return the {@link ProxyContext}.
     */
    @Override
    public ProxyContext getProxyContext() {
        return proxyContext;
    }

    /**
     * Returns the proxy instance on which the method is invoked.
     *
     * @return the proxy instance.
     */
    @Override
    public Object getProxy() {
        return proxy;
    }
}
