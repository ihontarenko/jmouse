package org.jmouse.core.bind.descriptor.structured.record;

import org.jmouse.core.bind.descriptor.AbstractDescriptor;
import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.reflection.TypeClassifier;

import java.util.Map;

public class ValueObjectDescriptor<T> extends AbstractDescriptor<T, ValueObjectData<T>, ValueObjectIntrospector<T>>
        implements TypeClassifier, ObjectDescriptor<T> {

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

    public Map<String, PropertyDescriptor<T>> getComponents() {
        return container.getComponents();
    }

    @Override
    public String toString() {
        return "[VO]: " + container.getType().getJavaType();
    }

}
