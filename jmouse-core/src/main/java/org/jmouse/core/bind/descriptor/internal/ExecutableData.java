package org.jmouse.core.bind.descriptor.internal;

import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;
import org.jmouse.core.bind.descriptor.ParameterDescriptor;

import java.lang.reflect.Executable;
import java.util.*;

abstract public class ExecutableData<M extends Executable> extends AnnotatedElementData<M> {

    private final Map<String, ParameterDescriptor> parameters     = new LinkedHashMap<>();
    private final List<ClassTypeDescriptor>        exceptionTypes = new ArrayList<>();

    public ExecutableData(M target) {
        super(target);
    }

    public Collection<ParameterDescriptor> getParameters() {
        return parameters.values();
    }

    public ParameterDescriptor getParameter(String name) {
        return parameters.get(name);
    }

    public void addParameter(ParameterDescriptor parameter) {
        parameters.put(parameter.getName(), parameter);
    }

    public void addExceptionType(ClassTypeDescriptor exceptionType) {
        exceptionTypes.add(exceptionType);
    }

    public List<ClassTypeDescriptor> getExceptionTypes() {
        return exceptionTypes;
    }

}
