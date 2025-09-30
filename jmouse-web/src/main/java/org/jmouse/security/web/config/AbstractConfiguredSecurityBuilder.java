package org.jmouse.security.web.config;

import org.jmouse.context.ApplicationBeanContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractConfiguredSecurityBuilder<T, B extends AbstractConfiguredSecurityBuilder<T, B>>
        extends AbstractSecurityBuilder<T> {

    private final List<SecurityConfigurer<B>> configurers = new ArrayList<>();
    private final Map<Class<?>, Object>       shared      = new HashMap<>();

    public <C extends SecurityConfigurer<B>> C apply(C configurer) {
        this.configurers.add(configurer);
        return configurer;
    }

    public <U> void setSharedObject(Class<U> type, U object) {
        shared.put(type, object);
    }

    @SuppressWarnings("unchecked")
    public <U> U getSharedObject(Class<U> type) {
        return (U) shared.get(type);
    }

    public ApplicationBeanContext getBeanContext() {
        return getSharedObject(ApplicationBeanContext.class);
    }

    @SuppressWarnings("unchecked")
    protected final void initializeConfigurers() throws Exception {
        for (SecurityConfigurer<B> configurer : configurers) {
            configurer.initialize((B) this);
        }
    }

    @SuppressWarnings("unchecked")
    protected final void configureConfigurers() throws Exception {
        for (SecurityConfigurer<B> configurer : configurers) {
            configurer.configure((B) this);
        }
    }
}
