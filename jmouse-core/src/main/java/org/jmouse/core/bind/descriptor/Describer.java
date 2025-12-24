package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanIntrospector;
import org.jmouse.core.bind.descriptor.structured.vo.ValueObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.vo.ValueObjectIntrospector;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

final public class Describer {

    private static final Map<Class<?>, ObjectDescriptor<?>> DESCRIPTORS = new HashMap<>();

    private Describer() {}

    public static ParameterDescriptor forParameter(final Parameter parameter) {
        return new ParameterIntrospector(parameter).introspect().toDescriptor();
    }

    public static MethodDescriptor forMethod(final Method method) {
        return new MethodIntrospector(method).introspect().toDescriptor();
    }

    public static ClassTypeDescriptor forClass(final Class<?> clazz) {
        return new ClassTypeIntrospector(clazz).introspect().toDescriptor();
    }

    public static String className(Class<?> type) {
        return Reflections.getShortName(type);
    }

    public static ObjectDescriptor<?> forJavaBean(final Class<?> type) {
        return DESCRIPTORS.computeIfAbsent(type, t -> new JavaBeanIntrospector<>(t)
                .introspect().toDescriptor());
    }

    public static ObjectDescriptor<?> forValueObject(final Class<?> type) {
        return DESCRIPTORS.computeIfAbsent(type, t -> new ValueObjectIntrospector<>(t)
                .introspect().toDescriptor());
    }

    @SuppressWarnings("unchecked")
    public static <T> ObjectDescriptor<T> forObjectDescriptor(final Class<T> type) {
        return type.isRecord() ? (ObjectDescriptor<T>) forValueObject(type) : (ObjectDescriptor<T>) forJavaBean(type);
    }

}
