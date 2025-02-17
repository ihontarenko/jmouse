package org.jmouse.core.bind.introspection.bean;

import org.jmouse.core.bind.introspection.AbstractDescriptor;

abstract public class PropertyDescriptor<T, I extends PropertyIntrospector<?, ?, ?>> extends AbstractDescriptor<T, PropertyData<T>, I> {

    protected PropertyDescriptor(I introspector, PropertyData<T> container) {
        super(introspector, container);
    }

    @Override
    public I toIntrospector() {
        return null;
    }

}
