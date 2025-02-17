package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.MethodData;

import java.lang.reflect.Method;

public class MethodDescriptor extends ExecutableDescriptor<Method, MethodData, MethodIntrospector> {

    protected MethodDescriptor(MethodIntrospector introspector, MethodData container) {
        super(introspector, container);
    }

    public ClassTypeDescriptor getReturnType() {
        return container.getReturnType();
    }

    @Override
    public MethodIntrospector toIntrospector() {
        return introspector;
    }

}
