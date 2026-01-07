package org.jmouse.jdbc.parameters;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.descriptor.Describer;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;

public final class BeanParameterSource implements ParameterSource {

    private final Object instance;
    private final ObjectDescriptor<Object> bean;

    @SuppressWarnings("unchecked")
    public BeanParameterSource(Object instance) {
        this.instance = Verify.nonNull(instance, "bean-instance");
        Class<?> beanType = instance.getClass();
        this.bean = Describer.forObjectDescriptor((Class<Object>) beanType);
    }

    @Override
    public boolean hasValue(int position) {
        return false;
    }

    @Override
    public boolean hasValue(String name) {
        return bean.hasProperty(name);
    }

    @Override
    public Object getValue(int position) {
        throw new UnsupportedOperationException("Positional parameters are not supported by BeanParameterSource");
    }

    @Override
    public Object getValue(String name) {
        return bean.obtainValue(name, instance);
    }
}
