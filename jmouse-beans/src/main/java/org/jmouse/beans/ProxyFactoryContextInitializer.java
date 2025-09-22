package org.jmouse.beans;

import org.jmouse.core.Priority;
import org.jmouse.core.proxy.*;

/**
 * 🏗️ {@link BeanContextInitializer} that sets up the proxy infrastructure.
 *
 * <p>Registers and wires together the components required for
 * annotation-driven proxy creation inside the {@link BeanContext}.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>📦 Register a shared {@link InterceptorRegistry} as a bean.</li>
 *   <li>⚙️ Register a {@link ProxyFactory} implementation
 *       ({@link DefaultProxyFactory}) that supports annotation-based interception.</li>
 *   <li>🔎 Trigger {@link InterceptorRegistrar} scanning to discover
 *       and register {@code @Intercept}-annotated interceptors.</li>
 * </ul>
 *
 * <h3>Ordering</h3>
 * <p>Annotated with {@link Priority} = {@code -100000}, ensuring this initializer
 * runs very early in the context bootstrap process.</p>
 */
@Priority(-100000)
public class ProxyFactoryContextInitializer implements BeanContextInitializer {

    @Override
    public void initialize(BeanContext context) {
        InterceptorRegistry registry = new InterceptorRegistry();
        // Register proxy factory that creates annotation-driven proxies for the scanned base classes.
        context.registerBean(InterceptorRegistry.class, registry);
        context.registerBean(ProxyFactory.class, new DefaultProxyFactory(registry));

        // Scanning and registering
        new InterceptorRegistrar(registry).register();
    }
}
