package org.jmouse.core.scope;

public interface BeanProviderAware {
    void setBeanProvider(BeanProvider beanProvider);
    BeanProvider getBeanProvider();
}
