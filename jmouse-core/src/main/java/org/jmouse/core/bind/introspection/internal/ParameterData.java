package org.jmouse.core.bind.introspection.internal;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;

import java.lang.reflect.Parameter;

public class ParameterData extends AnnotatedElementData<Parameter> {

    private ClassTypeDescriptor type;

    public ParameterData(Parameter target) {
        super(target);
    }

    public ClassTypeDescriptor getType() {
        return type;
    }

    public void setType(ClassTypeDescriptor type) {
        this.type = type;
    }
}
