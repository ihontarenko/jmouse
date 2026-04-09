package org.jmouse.jdbc.parameters;

import org.jmouse.core.Verify;
import org.jmouse.core.access.descriptor.Describer;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;

public final class BeanParameterSource implements ParameterSource {

    private final Object                   instance;
    private final ObjectDescriptor<Object> descriptor;

    @SuppressWarnings("unchecked")
    public BeanParameterSource(Object instance) {
        this.instance = Verify.nonNull(instance, "bean-instance");
        Class<?> beanType = instance.getClass();
        this.descriptor = Describer.forObjectDescriptor((Class<Object>) beanType);
    }

    @Override
    public boolean hasValue(int position) {
        return false;
    }

    @Override
    public boolean hasValue(String name) {
        return descriptor.hasProperty(name);
    }

    @Override
    public Object getValue(int position) {
        throw new UnsupportedOperationException("Positional parameters are not supported by BeanParameterSource");
    }

    @Override
    public Object getValue(String name) {
        return descriptor.obtainValue(name, instance);
    }
}
