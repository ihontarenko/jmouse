package org.jmouse.core.proxy;

/**
 * 🏭 Factory interface for creating proxies and their {@link ProxyDefinition}.
 *
 * <p>Provides high-level entrypoints for building proxied objects,
 * with optional interception and custom behaviors.</p>
 */
public interface ProxyFactory {

    /**
     * ✨ Create a proxy for the given instance using default definition building.
     *
     * <p>Delegates to {@link #createProxyDefinition(Object)} and then to
     * {@link #createProxy(ProxyDefinition)}.</p>
     *
     * @param <T>      expected proxy type
     * @param instance target object to proxy
     * @return proxy wrapping {@code instance}
     */
    default <T> T createProxy(Object instance) {
        return createProxy(createProxyDefinition(instance));
    }

    /**
     * 🧬 Create a proxy using an explicit {@link ProxyDefinition}.
     *
     * @param definition proxy definition
     * @param <T>        proxy type
     * @return generated proxy
     */
    <T> T createProxy(ProxyDefinition<T> definition);

    /**
     * 📦 Build a {@link ProxyDefinition} from a raw target instance.
     *
     * <p>This definition will typically contain target class, interceptors,
     * class loader, and other metadata needed for proxy creation.</p>
     *
     * @param instance target object
     * @param <T>      inferred type
     * @return proxy definition
     */
    <T> ProxyDefinition<T> createProxyDefinition(Object instance);

    /**
     * ➕ Register a global {@link MethodInterceptor}.
     *
     * <p>Interceptors added here will be applied to all proxies created
     * by this factory (depending on matching rules).</p>
     *
     * @param interceptor interceptor to add
     */
    void addInterceptor(MethodInterceptor interceptor);

    /**
     * 🔎 Access the interceptor registry associated with this factory.
     *
     * @return global interceptor registry
     */
    InterceptorRegistry getRegistry();
}
