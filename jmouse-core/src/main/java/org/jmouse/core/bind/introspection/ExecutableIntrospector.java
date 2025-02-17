package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.ExecutableData;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

abstract public class ExecutableIntrospector<C extends ExecutableData<E>, I extends ExecutableIntrospector<?, ?, ?, ?>, E extends Executable, D extends ExecutableDescriptor<?, ?, ?>> extends AnnotatedElementIntrospector<C, I, E, D> {

    protected ExecutableIntrospector(E target) {
        super(target);
    }

    @Override
    public I name() {
        return name(Reflections.getMethodName(container.getTarget()));
    }

    public I parameter(ParameterDescriptor descriptor) {
        container.addParameter(descriptor);
        return self();
    }

    public I parameters() {
        Parameter[] parameters = container.getTarget().getParameters();

        for (Parameter parameter : parameters) {
            parameter(new ParameterIntrospector(parameter).introspect().toDescriptor());
        }

        return self();
    }

    @Override
    public I introspect() {
        name().annotations();
        return self();
    }

}
