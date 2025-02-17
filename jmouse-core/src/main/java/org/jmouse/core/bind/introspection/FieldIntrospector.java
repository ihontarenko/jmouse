package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.FieldData;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Field;

public class FieldIntrospector extends AnnotatedElementIntrospector<FieldData, FieldIntrospector, Field, FieldDescriptor> {

    protected FieldIntrospector(Field target) {
        super(target);
    }

    @Override
    public FieldIntrospector name() {
        return name(Reflections.getFieldName(container.getTarget()));
    }

    @Override
    public FieldIntrospector introspect() {
        return annotations().name().type();
    }

    public FieldIntrospector type() {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(JavaType.forField(container.getTarget()));
        container.setType(introspector.annotations().name().toDescriptor());
        return self();
    }

    @Override
    public FieldDescriptor toDescriptor() {
        return new FieldDescriptor(this, container);
    }

    @Override
    public FieldData getContainerFor(Field target) {
        return new FieldData(target);
    }

}
