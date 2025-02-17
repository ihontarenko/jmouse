package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.ParameterData;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Parameter;

public class ParameterIntrospector extends AnnotatedElementIntrospector<ParameterData, ParameterIntrospector, Parameter, ParameterDescriptor> {

    protected ParameterIntrospector(Parameter target) {
        super(target);
    }

    @Override
    public ParameterIntrospector name() {
        return name(Reflections.getParameterName(container.getTarget()));
    }

    public ParameterIntrospector type() {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(JavaType.forParameter(container.getTarget()));
        container.setType(introspector.name().annotations().toDescriptor());
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
