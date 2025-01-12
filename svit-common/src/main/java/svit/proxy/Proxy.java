package svit.proxy;

/**
 * Interface representing a proxy in the system.
 * <p>
 * Proxies are used to provide dynamic behavior, such as intercepting method calls
 * or delegating operations to other objects.
 */
public interface Proxy {

    /**
     * Retrieves the proxy instance using the current thread's context class loader.
     * <p>
     * This is a convenience method that delegates to {@link #getProxy(ClassLoader)}
     * with the {@link Thread#getContextClassLoader()}.
     *
     * @param <T> the type of the proxy instance.
     * @return the proxy instance.
     */
    default <T> T getProxy() {
        return getProxy(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Retrieves the proxy instance using the specified {@link ClassLoader}.
     *
     * @param classLoader the class loader to use for creating the proxy.
     * @param <T>         the type of the proxy instance.
     * @return the proxy instance.
     */
    <T> T getProxy(ClassLoader classLoader);

    /**
     * Retrieves the {@link ProxyEngine} associated with this proxy.
     * <p>
     * The proxy engine is responsible for managing the creation and behavior of
     * proxies.
     *
     * @return the proxy engine.
     */
    ProxyEngine getProxyEngine();

}
