package org.jmouse.core.bind.introspection.bean;

import org.jmouse.core.bind.introspection.AbstractIntrospector;

abstract public class PropertyIntrospector<I extends PropertyIntrospector<I, T, D>, T, D extends PropertyDescriptor<T, I>>
        extends AbstractIntrospector<PropertyData<T>, I, T, D> {

    protected PropertyIntrospector(T target) {
        super(target);
    }

    @Override
    public I introspect() {
        return name();
    }

}
