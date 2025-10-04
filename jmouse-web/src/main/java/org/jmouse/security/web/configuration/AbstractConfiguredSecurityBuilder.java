package org.jmouse.security.web.configuration;

import org.jmouse.beans.BeanNotFoundException;
import org.jmouse.beans.BeanProvider;
import org.jmouse.core.Streamable;
import org.jmouse.web.context.WebBeanContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractConfiguredSecurityBuilder<T, B extends AbstractConfiguredSecurityBuilder<T, B>>
        extends AbstractSecurityBuilder<T> implements BeanProvider {

    private final List<SecurityConfigurer<T, B>> configurers = new ArrayList<>();
    private final Map<Class<?>, Object>          shared      = new HashMap<>();

    public <C extends SecurityConfigurer<T, B>> C apply(C configurer) {
        addConfigurer(configurer);
        return configurer;
    }

    @SuppressWarnings("unchecked")
    public <C extends ConfigurerAdapter<T, B>> C apply(C configurer) {
        addConfigurer(configurer);
        configurer.setBuilder((B) this);
        configurer.setBeanProvider(this);
        return configurer;
    }

    public <C extends SecurityConfigurer<T, B>> void addConfigurer(C configurer) {
        this.configurers.add(configurer);
    }

    public <C extends SecurityConfigurer<T, B>> void removeConfigurer(Class<C> type) {
        this.configurers.removeIf(type::isInstance);
    }

    @SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
    public <C extends SecurityConfigurer<T, B>> C getConfigurer(Class<C> type) {
        return (C) Streamable.of(this.configurers)
                .filter(type::isInstance).findFirst().get();
    }

    public <U> void setSharedObject(Class<U> type, U object) {
        shared.put(type, object);
    }

    @SuppressWarnings("unchecked")
    public <U> U getSharedObject(Class<U> type) {
        return (U) shared.get(type);
    }

    public WebBeanContext getBeanContext() {
        return getSharedObject(WebBeanContext.class);
    }

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

    @Override
    public <U> U getBean(Class<U> beanClass) {
        return getBeanContext().getBean(beanClass);
    }

    @SuppressWarnings("unchecked")
    protected final void initializeConfigurers() throws Exception {
        for (SecurityConfigurer<T, B> configurer : configurers) {
            configurer.initialize((B) this);
        }
    }

    @SuppressWarnings("unchecked")
    protected final void configureConfigurers() throws Exception {
        for (SecurityConfigurer<T, B> configurer : configurers) {
            configurer.configure((B) this);
        }
    }
}
