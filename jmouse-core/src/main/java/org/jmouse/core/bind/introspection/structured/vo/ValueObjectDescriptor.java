package org.jmouse.core.bind.introspection.structured.vo;

import org.jmouse.core.bind.introspection.AbstractDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;
import org.jmouse.core.reflection.ClassTypeInspector;

import java.util.Map;

public class ValueObjectDescriptor<T> extends AbstractDescriptor<T, ValueObjectData<T>, ValueObjectIntrospector<T>>
        implements ClassTypeInspector, ObjectDescriptor<T> {

    protected ValueObjectDescriptor(ValueObjectIntrospector<T> introspector, ValueObjectData<T> container) {
        super(introspector, container);
    }

    @Override
    public ValueObjectIntrospector<T> toIntrospector() {
        return introspector;
    }

    @Override
    public ClassTypeDescriptor getType() {
        return container.getType();
    }

    @Override
    public void setType(ClassTypeDescriptor type) {
        container.setType(type);
    }

    @Override
    public Map<String, PropertyDescriptor<T>> getProperties() {
        return container.getProperties();
    }

    @Override
    public Class<?> getClassType() {
        return getType().getClassType();
    }

    @Override
    public String toString() {
        return "[VO]: " + container.getType().getJavaType();
    }
}
