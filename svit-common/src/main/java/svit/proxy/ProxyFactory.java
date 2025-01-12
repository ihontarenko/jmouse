package svit.proxy;

/**
 * Factory for creating proxy instances with configurable behavior.
 * <p>
 * The {@code ProxyFactory} is responsible for setting up proxy configurations,
 * adding method interceptors, and generating proxy instances using a specified class loader.
 */
public class ProxyFactory implements Proxy {

    /**
     * The configuration used to create proxies.
     */
    protected final ProxyConfig proxyConfig;

    /**
     * Constructs a {@code ProxyFactory} for the given target object.
     *
     * @param target the target object to be proxied.
     */
    public ProxyFactory(Object target) {
        this.proxyConfig = new ProxyConfig(target);
    }

    /**
     * Adds a {@link MethodInterceptor} to the proxy configuration.
     * <p>
     * Interceptors are used to intercept method calls on the proxy and add custom behavior.
     *
     * @param interceptor the method interceptor to add.
     */
    public void addInterceptor(MethodInterceptor interceptor) {
        this.proxyConfig.addInterceptor(interceptor);
    }

    /**
     * Retrieves the proxy instance using the specified {@link ClassLoader}.
     *
     * @param classLoader the class loader to use for creating the proxy.
     * @param <T>         the type of the proxy instance.
     * @return the proxy instance.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(ClassLoader classLoader) {
        return (T) new JdkProxy(this.proxyConfig).getProxy(classLoader);
    }

    /**
     * Retrieves the {@link ProxyEngine} associated with this proxy.
     * <p>
     * This implementation does not support retrieving the proxy engine and throws an exception.
     *
     * @return never returns a value, always throws {@link UnsupportedOperationException}.
     * @throws UnsupportedOperationException if called.
     */
    @Override
    public ProxyEngine getProxyEngine() {
        throw new UnsupportedOperationException("Proxy engine is unknown and unsupported in this implementation.");
    }
}
