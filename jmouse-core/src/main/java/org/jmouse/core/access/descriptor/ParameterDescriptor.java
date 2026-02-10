package org.jmouse.core.access.descriptor;

import org.jmouse.core.access.descriptor.internal.ParameterData;

import java.lang.reflect.Parameter;

public class ParameterDescriptor extends AnnotatedElementDescriptor<Parameter, ParameterData, ParameterIntrospector> {

    public ParameterDescriptor(ParameterIntrospector introspector, ParameterData container) {
        super(introspector, container);
    }

    public ClassTypeDescriptor getType() {
        return container.getType();
    }

    public ExecutableDescriptor<?, ?, ?> getExecutable() {
        return container.getExecutable();
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
