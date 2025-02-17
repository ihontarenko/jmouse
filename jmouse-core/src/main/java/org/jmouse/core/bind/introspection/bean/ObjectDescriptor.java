package org.jmouse.core.bind.introspection.bean;

import org.jmouse.core.bind.PropertyAccessor;
import org.jmouse.core.bind.introspection.AbstractDescriptor;

abstract public class ObjectDescriptor<T, I extends ObjectIntrospector<?, ?, ?>> extends AbstractDescriptor<T, ObjectData<T>, I> {

    protected ObjectDescriptor(I introspector, ObjectData<T> container) {
        super(introspector, container);
    }

    public PropertyAccessor<T> getPropertyAccessor() {
        return null;
    }

}
