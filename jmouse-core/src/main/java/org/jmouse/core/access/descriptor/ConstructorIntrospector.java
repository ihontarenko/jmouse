package org.jmouse.core.access.descriptor;

import org.jmouse.core.access.descriptor.internal.ConstructorData;

import java.lang.reflect.Constructor;

public class ConstructorIntrospector extends ExecutableIntrospector<ConstructorData, ConstructorIntrospector, Constructor<?>, ConstructorDescriptor> {

    protected ConstructorIntrospector(Constructor<?> target) {
        super(target);
    }

    @Override
    public ConstructorIntrospector introspect() {
        return super.introspect();
    }

    @Override
    public ConstructorDescriptor toDescriptor() {
        return getCachedDescriptor(() -> new ConstructorDescriptor(this, container));
    }

    @Override
    public ConstructorData getContainerFor(Constructor<?> target) {
        return new ConstructorData(target);
    }

}
