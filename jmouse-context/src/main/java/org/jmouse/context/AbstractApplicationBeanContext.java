package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanNotFoundException;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.context.processor.ResourceInjectionBeanPostProcessor;
import org.jmouse.core.env.Environment;
import org.jmouse.core.env.PropertySource;
import org.jmouse.core.io.ResourceLoader;

import java.util.Collection;

public class AbstractApplicationBeanContext extends DefaultBeanContext implements ApplicationBeanContext {

    public AbstractApplicationBeanContext(BeanContext parent) {
        super(parent);
    }

    public AbstractApplicationBeanContext(Class<?>... baseClasses) {
        super(baseClasses);

        addBeanPostProcessor(new ResourceInjectionBeanPostProcessor());
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
    public Object getRawProperty(String name) {
        return getEnvironment().getProperty(name);
    }

    @Override
    public <T> T getProperty(String name, Class<T> targetType) {
        return getEnvironment().getProperty(name, targetType);
    }

    @Override
    public PropertySource<?> getPropertySource(String name) {
        return getEnvironment().getPropertySource(name);
    }

    @Override
    public void addPropertySource(PropertySource<?> propertySource) {
        getEnvironment().addPropertySource(propertySource);
    }

    @Override
    public boolean hasPropertySource(String name) {
        return getEnvironment().hasPropertySource(name);
    }

    @Override
    public boolean removePropertySource(String name) {
        return getEnvironment().removePropertySource(name);
    }

    @Override
    public Collection<? extends PropertySource<?>> getPropertySources() {
        return getEnvironment().getPropertySources();
    }
}
