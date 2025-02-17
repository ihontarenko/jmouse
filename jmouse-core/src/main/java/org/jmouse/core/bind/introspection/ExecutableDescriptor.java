package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.ExecutableData;

import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract public class ExecutableDescriptor<E extends Executable, C extends ExecutableData<E>, I extends ExecutableIntrospector<?, ?, ?, ?>> extends AnnotatedElementDescriptor<E, C, I> {

    protected ExecutableDescriptor(I introspector, C container) {
        super(introspector, container);
    }

    public Collection<ParameterDescriptor> getParameters() {
        return Collections.unmodifiableCollection(container.getParameters());
    }

    public ParameterDescriptor getParameter(String name) {
        return container.getParameter(name);
    }

    public ParameterDescriptor getParameter(int index) {
        ParameterDescriptor descriptor = null;

        if (index >= 0 && index < getParameters().size()) {
            descriptor = List.copyOf(getParameters()).get(index);
        }

        return descriptor;
    }

    public List<ClassTypeDescriptor> getExceptionTypes() {
        return Collections.unmodifiableList(container.getExceptionTypes());
    }

}
