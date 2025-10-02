package org.jmouse.security.web.configuration;

import org.jmouse.beans.BeanProvider;

public class ConfigurerAdapter<T, B extends SecurityBuilder<T>> implements SecurityConfigurer<T, B> {

    private B            builder;
    private BeanProvider beanProvider;

    public B getBuilder() {
        return builder;
    }

    public void setBuilder(B builder) {
        this.builder = builder;
    }

    public BeanProvider getBeanProvider() {
        return beanProvider;
    }

    public void setBeanProvider(BeanProvider beanProvider) {
        this.beanProvider = beanProvider;
    }

}
