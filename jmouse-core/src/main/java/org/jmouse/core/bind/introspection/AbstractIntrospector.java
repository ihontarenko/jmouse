package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.DataContainer;

abstract class AbstractIntrospector<C extends DataContainer<T>, I extends Introspector<?, ?, ?, ?>, T, D extends Descriptor<?, ?, ?>>
        implements Introspector<C, I, T, D> {

    protected C container;

    protected AbstractIntrospector(T target) {
        this.container = getContainerFor(target);
    }

    @Override
    public I name(String name) {
        this.container.setName(name);
        return self();
    }

}
