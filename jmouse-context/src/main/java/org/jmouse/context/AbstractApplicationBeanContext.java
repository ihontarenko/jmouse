package org.jmouse.context;

import svit.beans.BeanContext;
import svit.beans.BeanNotFoundException;
import svit.beans.DefaultBeanContext;
import org.jmouse.core.env.Environment;
import org.jmouse.core.env.PropertySourceRegistry;
import org.jmouse.core.io.ResourceLoader;

public class AbstractApplicationBeanContext extends DefaultBeanContext implements ApplicationBeanContext {

    public AbstractApplicationBeanContext(BeanContext parent) {
        super(parent);
    }

    public AbstractApplicationBeanContext(Class<?>... baseClasses) {
        super(baseClasses);
    }

    @Override
    public Environment getEnvironment() {
        try {
            return getBean(Environment.class);
        } catch (BeanNotFoundException exception) {
            throw new RuntimeException(
                    "If the environment is not available in the context '%s', it means that the context has not yet been initialized properly."
                            .formatted(getContextId()));
        }
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return getBean(ResourceLoader.class);
    }

    @Override
    public void setRegistry(PropertySourceRegistry registry) {
        throw new UnsupportedOperationException("Direct call from context is not supported");
    }

    @Override
    public PropertySourceRegistry getRegistry() {
        throw new UnsupportedOperationException("Direct call from context is not supported");
    }

    @Override
    public Object getRawProperty(String name) {
        return getEnvironment().getProperty(name);
    }

    @Override
    public <T> T getProperty(String name, Class<T> targetType) {
        return getEnvironment().getProperty(name, targetType);
    }

}
