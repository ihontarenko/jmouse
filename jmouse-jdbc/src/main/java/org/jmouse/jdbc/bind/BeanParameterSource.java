package org.jmouse.jdbc.bind;

import org.jmouse.core.Contract;
import org.jmouse.core.bind.descriptor.Describer;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;

/**
 * {@link ParameterSource} implementation that exposes bean properties
 * as named parameters.
 * <p>
 * Property metadata is resolved via {@link ObjectDescriptor}, allowing
 * structured access to bean properties.
 * </p>
 */
public final class BeanParameterSource implements ParameterSource {

    private final Object                   instance;
    private final ObjectDescriptor<Object> bean;

    /**
     * Create a new {@code BeanParameterSource} for the given bean instance.
     *
     * @param instance the target bean instance (never {@code null})
     */
    @SuppressWarnings("unchecked")
    public BeanParameterSource(Object instance) {
        this.instance = Contract.nonNull(instance, "bean-instance");
        Class<?> beanType = instance.getClass();
        this.bean = Describer.forObjectDescriptor((Class<Object>) beanType);
    }

    /**
     * Determine whether the given property is available on the target bean.
     *
     * @param name the parameter name
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    @Override
    public boolean hasValue(String name) {
        return bean.hasProperty(name);
    }

    /**
     * Return the value of the specified bean property.
     *
     * @param name the parameter name
     * @return the current property value
     */
    @Override
    public Object getValue(String name) {
        return bean.obtainValue(name, instance);
    }

}

