package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.DataContainer;

abstract public class AbstractDescriptor<T, C extends DataContainer<T>, I extends Introspector<?, ?, ?, ?>> implements Descriptor<T, C, I> {

    protected final C container;
    protected final I introspector;

    protected AbstractDescriptor(I introspector, C container) {
        this.introspector = introspector;
        this.container = container;
    }

    @Override
    public String getName() {
        return container.getName();
    }

    @Override
    public T unwrap() {
        return container.getTarget();
    }

}
