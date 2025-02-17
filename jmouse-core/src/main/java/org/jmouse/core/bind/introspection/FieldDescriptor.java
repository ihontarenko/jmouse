package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.FieldData;

import java.lang.reflect.Field;

public class FieldDescriptor extends AnnotatedElementDescriptor<Field, FieldData, FieldIntrospector> {

    protected FieldDescriptor(FieldIntrospector introspector, FieldData container) {
        super(introspector, container);
    }

    @Override
    public FieldIntrospector toIntrospector() {
        return introspector;
    }

}
