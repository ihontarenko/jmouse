package org.jmouse.common.support.context;

public interface BeanProviderAware {
    void setBeanProvider(BeanProvider beanProvider);
    BeanProvider getBeanProvider();
}
