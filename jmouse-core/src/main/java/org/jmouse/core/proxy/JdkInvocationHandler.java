package org.jmouse.core.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 📦 JDK dynamic-proxy {@link InvocationHandler} implementation.
 *
 * <p>Acts as the glue between the JDK proxy mechanism and the framework’s
 * {@link ProxyDispatcher} abstraction. All method calls on the generated proxy
 * are routed to the dispatcher for interception and pipeline execution.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Delegate all calls to {@link ProxyDispatcher#invoke(Object, Method, Object[])}</li>
 *   <li>Expose {@link ProxyDefinition} via {@link ProxyIntrospection}</li>
 * </ul>
 *
 * <p>Used internally when proxies are created via {@link java.lang.reflect.Proxy}.</p>
 */
public class JdkInvocationHandler implements InvocationHandler, ProxyIntrospection {

    private final ProxyDispatcher    dispatcher;
    private final ProxyDefinition<?> definition;

    /**
     * Creates a new JDK invocation handler.
     *
     * @param dispatcher dispatcher handling proxy calls
     * @param definition definition of the proxy being managed
     */
    public JdkInvocationHandler(ProxyDispatcher dispatcher, ProxyDefinition<?> definition) {
        this.dispatcher = dispatcher;
        this.definition = definition;
    }

    /**
     * Delegates method invocations from the proxy instance to the dispatcher.
     *
     * @param proxy     the proxy instance
     * @param method    the method being invoked
     * @param arguments the arguments passed to the method (may be {@code null})
     * @return the result of the invocation
     * @throws Throwable if the target or interceptors throw
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        return dispatcher.invoke(proxy, method, arguments);
    }

    /**
     * @return the proxy definition associated with this handler
     */
    @Override
    public ProxyDefinition<?> getProxyDefinition() {
        return definition;
    }
}
