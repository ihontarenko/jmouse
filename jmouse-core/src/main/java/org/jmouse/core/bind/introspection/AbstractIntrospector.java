package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.DataContainer;

import java.util.function.Supplier;

abstract public class AbstractIntrospector<C extends DataContainer<T>, I extends Introspector<?, ?, ?, ?>, T, D extends Descriptor<?, ?, ?>>
        implements Introspector<C, I, T, D> {

    protected C container;
    protected D descriptor;

    protected AbstractIntrospector(T target) {
        this.container = getContainerFor(target);
    }

    @Override
    public I name(String name) {
        this.container.setName(name);
        return self();
    }

    protected D getDescriptor(Supplier<D> factory) {
        D descriptor = this.descriptor;

        if (descriptor == null) {
            descriptor = factory.get();
        }

        return descriptor;
    }

}
