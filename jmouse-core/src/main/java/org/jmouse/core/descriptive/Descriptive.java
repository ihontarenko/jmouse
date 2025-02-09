package org.jmouse.core.descriptive;

import org.jmouse.common.support.invocable.FieldDescriptor;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.CyclicReferenceDetector;
import org.jmouse.util.DefaultCyclicReferenceDetector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.Reflections.getShortName;

final public class Descriptive {

    private static final Supplier<CyclicReferenceMappingException> CYCLIC_REFERENCE_EXCEPTION
            = () -> new CyclicReferenceMappingException("Cyclic reference detected");
    private static final CyclicReferenceDetector<Class<?>>         DETECTOR
            = new DefaultCyclicReferenceDetector<>();

    public static ParameterDescriptor forParameter(Parameter parameter) {
        ParameterDescriptor.Builder builder = new ParameterDescriptor.Builder(parameter.getName());

        for (Annotation annotation : parameter.getAnnotations()) {
            builder.annotation(forAnnotation(annotation));
        }

        builder.type(forClass(parameter.getType(), false));
        builder.internal(parameter);

        return builder.build();
    }

    public static MethodDescriptor forMethod(Method method) {
        MethodDescriptor.Builder builder = new MethodDescriptor.Builder(Reflections.getMethodName(method));

        builder.internal(method).returnType(forClass(method.getReturnType(), false));

        for (Parameter parameter : method.getParameters()) {
            builder.parameters(forParameter(parameter));
        }

        for (Class<?> exceptionType : method.getExceptionTypes()) {
            builder.exceptionTypes(forClass(exceptionType, false));
        }

        return builder.build();
    }

    public static FieldDescriptor forField(Field field) {
        return null; // todo: not implemented yet
    }

    public static ConstructorDescriptor forConstructor(Constructor<?> constructor) {
        return null; // todo: not implemented yet
    }

    public static ClassDescriptor forClass(Class<?> type) {
        return forClass(type, true);
    }

    public static ClassDescriptor forClass(Class<?> type, boolean nested) {
        ClassDescriptor.Builder builder = new ClassDescriptor.Builder(getShortName(type));

        DETECTOR.detect(() -> type, CYCLIC_REFERENCE_EXCEPTION);

        builder.internal(type);

        if (nested) {
            for (Annotation annotation : type.getAnnotations()) {
                builder.annotation(forAnnotation(annotation));
            }

            for (Method method : type.getDeclaredMethods()) {
                builder.method(forMethod(method));
            }

            for (Constructor<?> ctor : type.getDeclaredConstructors()) {
                builder.constructor(forConstructor(ctor));
            }

            for (Field field : type.getDeclaredFields()) {
                builder.field(forField(field));
            }
        }

        DETECTOR.remove(() -> type);

        return builder.build();
    }

    public static AnnotationDescriptor forAnnotation(Annotation annotation) {
        Class<? extends Annotation>  type    = annotation.annotationType();
        AnnotationDescriptor.Builder builder = new AnnotationDescriptor.Builder(getShortName(type));

        builder.internal(annotation).annotationType(forClass(type, false));

        try {
            Map<String, Object> attributes = new HashMap<>();

            for (Method method : type.getDeclaredMethods()) {
                attributes.put(method.getName(), method.invoke(annotation));
            }

            builder.attributes(attributes);
        } catch (Exception ignored) {}

        return builder.build();
    }

}
