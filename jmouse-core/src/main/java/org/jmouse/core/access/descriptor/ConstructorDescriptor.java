package org.jmouse.core.access.descriptor;

import org.jmouse.core.access.descriptor.internal.ConstructorData;

import java.lang.reflect.Constructor;

public class ConstructorDescriptor extends ExecutableDescriptor<Constructor<?>, ConstructorData, ConstructorIntrospector> {

    protected ConstructorDescriptor(ConstructorIntrospector introspector, ConstructorData container) {
        super(introspector, container);
    }

    @Override
    public ConstructorIntrospector toIntrospector() {
        return introspector;
    }

}
