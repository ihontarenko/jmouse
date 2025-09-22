package org.jmouse.core.proxy;

/**
 * 🏗️ Base implementation of {@link ProxyFactory}.
 *
 * <p>Provides common logic for creating proxies from plain target objects
 * by adapting them into a {@link ProxyDefinition}. Concrete subclasses
 * decide how to choose and apply {@link ProxyEngine}s.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>⚡ Convert raw target objects into a default {@link ProxyDefinition}.</li>
 *   <li>🪝 Expose the {@link InterceptorRegistry} used to manage global interceptors.</li>
 *   <li>🔧 Delegate actual proxy creation to {@link #createProxy(ProxyDefinition)}.</li>
 * </ul>
 */
public abstract class AbstractProxyFactory implements ProxyFactory {

    /**
     * Central registry of interceptors (shared by this factory).
     */
    protected final InterceptorRegistry registry;

    /**
     * Create a new abstract proxy factory with the given interceptor registry.
     *
     * @param registry global interceptor registry
     */
    protected AbstractProxyFactory(InterceptorRegistry registry) {
        this.registry = registry;
    }

    /**
     * 📦 Build a {@link ProxyDefinition} from a raw target instance.
     *
     * <p>This definition will typically contain target class, interceptors,
     * class loader, and other metadata needed for proxy creation.</p>
     *
     * @param instance target object
     * @return proxy definition
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> ProxyDefinition<T> createProxyDefinition(Object instance) {
        Class<T>            type             = (Class<T>) instance.getClass();
        InterceptorRegistry registry         = this.registry;
        InstanceProvider<T> instanceProvider = InstanceProvider.singleton((T) instance);

        ProxyDefinition.Builder<T> builder = ProxyDefinition.builder(type)
                .classLoader(type.getClassLoader())
                .mixins(Mixins.of(instance))
                .policy(InterceptionPolicy.defaultPolicy())
                .instanceProvider(instanceProvider);

        builder.interceptAll(registry.select(type));

        return builder.build();
    }

    /**
     * 🔎 Access the interceptor registry associated with this factory.
     *
     * @return global interceptor registry
     */
    public InterceptorRegistry getRegistry() {
        return registry;
    }
}
