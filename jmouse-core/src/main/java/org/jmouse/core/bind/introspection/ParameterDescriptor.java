package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.ParameterData;

import java.lang.reflect.Parameter;

public class ParameterDescriptor extends AnnotatedElementDescriptor<Parameter, ParameterData, ParameterIntrospector> {

    public ParameterDescriptor(ParameterIntrospector introspector, ParameterData container) {
        super(introspector, container);
    }

    @Override
    public ParameterIntrospector toIntrospector() {
        return introspector;
    }

}
