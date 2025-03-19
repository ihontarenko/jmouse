package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.internal.ParameterData;

import java.lang.reflect.Parameter;

public class ParameterDescriptor extends AnnotatedElementDescriptor<Parameter, ParameterData, ParameterIntrospector> {

    public ParameterDescriptor(ParameterIntrospector introspector, ParameterData container) {
        super(introspector, container);
    }

    public ClassTypeDescriptor getType() {
        return container.getType();
    }

    @Override
    public ParameterIntrospector toIntrospector() {
        return introspector;
    }

    @Override
    public String toString() {
        return "[%s]: %s".formatted(getName(), getType().getJavaType());
    }
}
