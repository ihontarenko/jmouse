package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.internal.ParameterData;
import org.jmouse.core.reflection.JavaType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class ParameterIntrospector
        extends AnnotatedElementIntrospector<ParameterData, ParameterIntrospector, Parameter, ParameterDescriptor> {

    public ParameterIntrospector(Parameter target) {
        super(target);
    }

    @Override
    public ParameterIntrospector name() {
        return name(container.getTarget().getName());
    }

    public ParameterIntrospector type() {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(JavaType.forParameter(container.getTarget()));
        container.setType(introspector.name().annotations().toDescriptor());
        return self();
    }

    public ParameterIntrospector executable(Executable executable) {
        ExecutableDescriptor<?, ?, ?> descriptor = null;

        if (executable instanceof Constructor<?> constructor) {
            descriptor = new ConstructorIntrospector(constructor).introspect().toDescriptor();
        } else if (executable instanceof Method method) {
            descriptor = new MethodIntrospector(method).introspect().toDescriptor();
        }

        return executable(descriptor);
    }

    public ParameterIntrospector executable(ExecutableDescriptor<?, ?, ?> executable) {
        container.setExecutable(executable);
        return self();
    }

    @Override
    public ParameterIntrospector introspect() {
        return name().type().annotations();
    }

    @Override
    public ParameterDescriptor toDescriptor() {
        return new ParameterDescriptor(this, container);
    }

    @Override
    public ParameterData getContainerFor(Parameter target) {
        return new ParameterData(target);
    }

}
