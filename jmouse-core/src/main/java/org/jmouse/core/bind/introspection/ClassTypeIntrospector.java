package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.ClassTypeData;
import org.jmouse.core.reflection.JavaType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClassTypeIntrospector extends AnnotatedElementIntrospector<ClassTypeData, ClassTypeIntrospector, Class<?>, ClassTypeDescriptor> {

    protected ClassTypeIntrospector(Class<?> type) {
        this(JavaType.forClass(type));
    }

    public ClassTypeIntrospector(JavaType type) {
        super(type.getRawType());
        type(type);
    }

    @Override
    public ClassTypeIntrospector name() {
        return name(Describer.className(container.getTarget()));
    }

    public ClassTypeIntrospector type() {
        return type(JavaType.forClass(container.getTarget()));
    }

    public ClassTypeIntrospector type(JavaType type) {
        container.setType(type);
        return self();
    }

    public ClassTypeIntrospector constructor(ConstructorDescriptor constructor) {
        container.addConstructor(constructor);
        return self();
    }

    public ClassTypeIntrospector constructors() {
        ClassTypeDescriptor parent = toDescriptor();

        for (Constructor<?> constructor : container.getTarget().getConstructors()) {
            constructor(new ConstructorIntrospector(constructor).introspect().toDescriptor());
        }

        return self();
    }

    public ClassTypeIntrospector field(FieldDescriptor field) {
        container.addField(field);
        return self();
    }

    public ClassTypeIntrospector fields() {
        ClassTypeDescriptor parent = toDescriptor();

        for (Field field : container.getTarget().getFields()) {
            field(new FieldIntrospector(field).introspect().toDescriptor());
        }

        return self();
    }

    public ClassTypeIntrospector method(MethodDescriptor method) {
        container.addMethod(method);
        return self();
    }

    public ClassTypeIntrospector methods() {
        ClassTypeDescriptor parent = toDescriptor();

        for (Method method : container.getTarget().getDeclaredMethods()) {
            method(new MethodIntrospector(method).introspect().toDescriptor());
        }

        return self();
    }

    @Override
    public ClassTypeIntrospector introspect() {
        return name().annotations().constructors().methods().fields();
    }

    @Override
    public ClassTypeDescriptor toDescriptor() {
        return getCachedDescriptor(() -> new ClassTypeDescriptor(this, container));
    }

    @Override
    public ClassTypeData getContainerFor(Class<?> target) {
        return new ClassTypeData(target);
    }

}
