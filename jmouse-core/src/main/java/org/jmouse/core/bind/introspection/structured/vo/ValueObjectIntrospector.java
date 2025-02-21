package org.jmouse.core.bind.introspection.structured.vo;

import org.jmouse.core.bind.introspection.*;
import org.jmouse.core.bind.introspection.structured.ObjectData;
import org.jmouse.core.reflection.JavaType;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;

public class ValueObjectIntrospector<T> extends AbstractIntrospector<ObjectData<T>, ValueObjectIntrospector<T>, T, ValueObjectDescriptor<T>> {

    public ValueObjectIntrospector(Class<T> target) {
        super(null);
        type(target);
    }

    public ValueObjectIntrospector<T> type(Class<T> type) {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(JavaType.forClass(type));
        container.setType(introspector.introspect().toDescriptor());
        return self();
    }

    public ValueObjectIntrospector<T> properties() {
        ClassTypeDescriptor   classTypeDescriptor   = container.getType();
        ConstructorDescriptor constructorDescriptor = classTypeDescriptor.getConstructors().getFirst();

        System.out.println(constructorDescriptor);
        for (ParameterDescriptor parameter : constructorDescriptor.getParameters()) {
            System.out.println(parameter);
        }

        return self();
    }

    @Override
    public ValueObjectIntrospector<T> name() {
        return name(container.getType().getName());
    }

    @Override
    public ValueObjectIntrospector<T> introspect() {
        return name().properties();
    }

    @Override
    public ValueObjectDescriptor<T> toDescriptor() {
        return getCachedDescriptor(() -> new ValueObjectDescriptor<>(this, container));
    }

    @Override
    public ObjectData<T> getContainerFor(T target) {
        return new ObjectData<>(target);
    }
}
