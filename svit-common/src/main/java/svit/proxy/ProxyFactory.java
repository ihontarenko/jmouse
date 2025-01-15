package svit.proxy;

import java.util.List;

/**
 * A factory interface for creating proxies and corresponding {@link ProxyContext}
 * instances. Implementations of this interface can generate proxy objects for
 * various use cases, such as method interception or custom behavior injection.
 */
public interface ProxyFactory {

    /**
     * Creates a proxy for the specified object.
     *
     * @param <T>   the type of the created proxy
     * @param object the original object to be proxied
     * @return a proxy instance of the specified object
     */
    <T> T createProxy(Object object);

    /**
     * Creates a {@link ProxyContext} for the specified object and class loader.
     *
     * @param object      the original object to be proxied
     * @param classLoader the class loader to define the proxy class
     * @return a new {@link ProxyContext} instance
     */
    ProxyContext createProxyContext(Object object, ClassLoader classLoader);

    /**
     * Adds a {@link MethodInterceptor} to this factory, which will be invoked
     * during method calls on the proxy object. Multiple interceptors can be added,
     * and each will be applied in the order they were registered.
     *
     * @param interceptor the interceptor to add
     */
    void addInterceptor(MethodInterceptor interceptor);

    /**
     * Retrieves the list of all currently registered {@link MethodInterceptor}s.
     * Implementations may return an unmodifiable list or a direct reference, so
     * modifications may or may not affect the actual interceptor list.
     *
     * @return a list of registered interceptors
     */
    List<MethodInterceptor> getInterceptors();
}
