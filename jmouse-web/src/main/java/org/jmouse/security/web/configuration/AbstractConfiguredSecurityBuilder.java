package org.jmouse.security.web.configuration;

import org.jmouse.beans.BeanNotFoundException;
import org.jmouse.beans.BeanProvider;
import org.jmouse.core.Streamable;
import org.jmouse.web.context.WebBeanContext;

import java.util.*;
import java.util.function.Supplier;

/**
 * 🧩 AbstractConfiguredSecurityBuilder
 *
 * Base builder that composes multiple {@link SecurityConfigurer}s, manages a shared
 * object map, and integrates with a DI context via {@link BeanProvider}.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>🧱 Apply/initialize/configure a sequence of {@link SecurityConfigurer}s</li>
 *   <li>🔗 Expose a shared-object registry for cross-configurer collaboration</li>
 *   <li>🌱 Resolve beans from {@link WebBeanContext} with optional fallbacks</li>
 * </ul>
 *
 * @param <T> the product type built by this builder
 * @param <B> the concrete builder subtype (for fluent API)
 */
public abstract class AbstractConfiguredSecurityBuilder<T, B extends AbstractConfiguredSecurityBuilder<T, B>>
        extends AbstractSecurityBuilder<T> implements BeanProvider {

    /**
     * 📚 Registered configurers in application order.
     */
    private final List<SecurityConfigurer<T, B>> configurers = new ArrayList<>();

    /**
     * 🗂️ Shared object registry (type → instance).
     */
    private final Map<Class<?>, Object>          shared      = new HashMap<>();

    /**
     * ➕ Apply a {@link SecurityConfigurer} as-is.
     *
     * @param configurer configurer to add
     * @return the same configurer (for further customization)
     */
    public <C extends SecurityConfigurer<T, B>> C apply(C configurer) {
        addConfigurer(configurer);
        return configurer;
    }

    /**
     * ➕ Apply a {@link ConfigurerAdapter}, wiring builder and bean provider.
     *
     * @param configurer adapter to add
     * @return the same adapter (for fluent tweaks)
     */
    @SuppressWarnings("unchecked")
    public <C extends ConfigurerAdapter<T, B>> C apply(C configurer) {
        addConfigurer(configurer);
        configurer.setBuilder((B) this);
        configurer.setBeanProvider(this);
        return configurer;
    }

    /**
     * 📥 Register a configurer (appended to the chain).
     *
     * @param configurer configurer to add
     */
    public <C extends SecurityConfigurer<T, B>> void addConfigurer(C configurer) {
        this.configurers.add(configurer);
    }

    /**
     * 🧹 Remove all configurers of the given type.
     *
     * @param type configurer class to remove
     */
    public <C extends SecurityConfigurer<T, B>> void removeConfigurer(Class<C> type) {
        this.configurers.removeIf(type::isInstance);
    }

    /**
     * 🔎 Get the first configurer of the given type.
     *
     * @param type configurer class
     * @return the first matching configurer
     * @throws java.util.NoSuchElementException if none present
     */
    @SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
    public <C extends SecurityConfigurer<T, B>> C getConfigurer(Class<C> type) {
        return (C) Streamable.of(this.configurers)
                .filter(type::isInstance).findFirst().get();
    }

    /**
     * 🗂️ Put a shared object (overwrites existing).
     *
     * @param type   key class
     * @param object instance to share
     */
    public <U> void setSharedObject(Class<U> type, U object) {
        shared.put(type, object);
    }

    /**
     * 🔎 Get a shared object by type.
     *
     * @param type key class
     * @return instance or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <U> U getSharedObject(Class<U> type) {
        return (U) shared.get(type);
    }

    /**
     * 🌱 Access the web bean context from shared objects.
     *
     * @return {@link WebBeanContext}
     */
    public WebBeanContext getBeanContext() {
        return getSharedObject(WebBeanContext.class);
    }

    /**
     * 🔧 Resolve (and cache) an object by:
     * <ol>
     *   <li>shared map</li>
     *   <li>DI bean lookup via {@link #getBean(Class)}</li>
     *   <li>fallback supplier (if provided)</li>
     * </ol>
     *
     * <p>If resolved, it is stored back into the shared map.</p>
     *
     * @param type          object type
     * @param defaultObject supplier used if not found
     * @return resolved instance (or {@code null} if all strategies fail)
     */
    public <U> U getObject(Class<U> type, Supplier<U> defaultObject) {
        U instance = getSharedObject(type);

        if (instance == null) {
            try {
                instance = getBean(type);
            } catch (BeanNotFoundException ignored) { }

            if (instance == null && defaultObject != null) {
                instance = defaultObject.get();
            }
        }

        if (instance != null) {
            setSharedObject(type, instance);
        }

        return instance;
    }

    /**
     * 🫘 BeanProvider: delegate to {@link WebBeanContext}.
     *
     * @param beanClass type to resolve
     * @return bean instance or throws if not found
     */
    @Override
    public <U> U getBean(Class<U> beanClass) {
        return getBeanContext().getBean(beanClass);
    }

    /**
     * 🚀 Lifecycle: initialize each registered configurer.
     *
     * @throws Exception any failure in a configurer's {@code initialize}
     */
    @SuppressWarnings("unchecked")
    protected final void initializeConfigurers() throws Exception {
        for (SecurityConfigurer<T, B> configurer : configurers) {
            configurer.initialize((B) this);
        }
    }

    /**
     * 🧭 Lifecycle: configure each registered configurer.
     *
     * @throws Exception any failure in a configurer's {@code configure}
     */
    @SuppressWarnings("unchecked")
    protected final void configureConfigurers() throws Exception {
        for (SecurityConfigurer<T, B> configurer : configurers) {
            configurer.configure((B) this);
        }
    }
}
