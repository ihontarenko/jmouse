package org.jmouse.core.bind.descriptor.structured;

import org.jmouse.core.bind.descriptor.AbstractDescriptor;
import org.jmouse.core.bind.descriptor.AbstractIntrospector;

abstract public class ObjectIntrospector<I extends ObjectIntrospector<?, ?, ?>, T, D extends AbstractDescriptor<?, ?, ?>> extends AbstractIntrospector<ObjectData<T>, I, T, D> {

    protected ObjectIntrospector(T target) {
        super(target);
    }

    @Override
    public I introspect() {
        return self();
    }

    @Override
    public ObjectData<T> getContainerFor(T target) {
        return new ObjectData<>(target);
    }

}
