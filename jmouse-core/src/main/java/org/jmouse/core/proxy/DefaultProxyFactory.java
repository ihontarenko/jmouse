package org.jmouse.core.proxy;

import org.jmouse.core.Streamable;
import org.jmouse.core.proxy.old.ByteBuddyProxyEngine;
import org.jmouse.core.proxy.old.JdkProxyEngine;

import java.util.List;

/**
 * 🏭 Default implementation of {@link ProxyFactory}.
 *
 * <p>Delegates proxy creation to one of the built-in {@link ProxyEngine}s:
 * <ul>
 *   <li>⚡ {@link JdkProxyEngine} — for interface-based proxies</li>
 *   <li>🧬 {@link ByteBuddyProxyEngine} — for subclass-based proxies</li>
 * </ul>
 * The first engine that {@link ProxyEngine#supports(ProxyDefinition) supports}
 * a given {@link ProxyDefinition} is chosen.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>📋 Maintains an {@link InterceptorRegistry} for global interceptors.</li>
 *   <li>➕ Allows runtime registration of additional interceptors.</li>
 *   <li>🚨 Throws {@link UnsupportedProxyException} if no engine can handle a target.</li>
 * </ul>
 */
public class DefaultProxyFactory extends AbstractProxyFactory {

    private final List<ProxyEngine> engines = List.of(
            new ByteBuddyProxyEngine(),
            new JdkProxyEngine()
    );

    /**
     * 🔧 Create a factory with a shared {@link InterceptorRegistry}.
     *
     * @param registry global interceptor registry
     */
    public DefaultProxyFactory(InterceptorRegistry registry) {
        super(registry);
    }

    /**
     * 🔧 Create a factory with an isolated registry and pre-registered interceptors.
     *
     * @param interceptors initial set of interceptors
     */
    public DefaultProxyFactory(MethodInterceptor... interceptors) {
        super(new InterceptorRegistry());
        Streamable.of(interceptors).forEach(this::addInterceptor);
    }

    /**
     * 🏗️ Create a proxy using the first compatible {@link ProxyEngine}.
     *
     * @param definition proxy definition
     * @param <T>        proxy type
     * @return generated proxy
     * @throws UnsupportedProxyException if no engine supports this definition
     */
    @Override
    public <T> T createProxy(ProxyDefinition<T> definition) {
        ProxyEngine engine = null;

        for (ProxyEngine candidate : engines) {
            if (candidate.supports(definition)) {
                engine = candidate;
                break;
            }
        }

        if (engine != null) {
            return engine.createProxy(definition);
        }

        throw new UnsupportedProxyException(
                "No proxy engine can handle target: '%s'. Provide interfaces (JDK) or use non-final class (ByteBuddy)."
                        .formatted(definition.targetClass()));
    }

    /**
     * ➕ Register a new interceptor into this factory's registry.
     *
     * @param interceptor interceptor to add
     */
    @Override
    public void addInterceptor(MethodInterceptor interceptor) {
        registry.register(interceptor);
    }
}
