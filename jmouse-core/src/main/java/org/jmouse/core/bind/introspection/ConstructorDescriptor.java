package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.ConstructorData;

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
