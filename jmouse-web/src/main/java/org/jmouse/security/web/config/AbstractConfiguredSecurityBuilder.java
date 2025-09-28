package org.jmouse.security.web.config;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class AbstractConfiguredSecurityBuilder<T, B extends AbstractConfiguredSecurityBuilder<T, B>>
        extends AbstractSecurityBuilder<T> {

    private final java.util.List<SecurityConfigurer<B>> configurers = new ArrayList<>();
    private final java.util.Map<Class<?>, Object>       shared      = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <C extends SecurityConfigurer<B>> B apply(C configurer) {
        this.configurers.add(configurer);
        return (B) this;
    }

    public <U> void setSharedObject(Class<U> type, U object) {
        shared.put(type, object);
    }

    @SuppressWarnings("unchecked")
    public <U> U getSharedObject(Class<T> type) {
        return (U) shared.get(type);
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
