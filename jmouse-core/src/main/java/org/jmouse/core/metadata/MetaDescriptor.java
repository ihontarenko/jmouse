package org.jmouse.core.metadata;

import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.CyclicReferenceDetector;
import org.jmouse.util.DefaultCyclicReferenceDetector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.Reflections.getShortName;

final public class MetaDescriptor {

    private static final Supplier<CyclicReferenceMappingException> CYCLIC_REFERENCE_EXCEPTION
                                                                             = () -> new CyclicReferenceMappingException("Cyclic reference detected");
    private static final CyclicReferenceDetector<Class<?>> DETECTOR
                                                                         = new DefaultCyclicReferenceDetector<>();
    public static final  int                               DEFAULT_DEPTH = 4;

    public static ParameterDescriptor forParameter(Parameter parameter) {
        return forParameter(parameter, DEFAULT_DEPTH);
    }

    private static ParameterDescriptor forParameter(Parameter parameter, int depth) {
        ParameterDescriptor.Builder builder = new ParameterDescriptor.Builder(parameter.getName());

        for (Annotation annotation : parameter.getAnnotations()) {
            builder.annotation(forAnnotation(annotation));
        }

        builder.type(forClass(parameter.getType(), depth - 1));
        builder.internal(parameter);

        return builder.build();
    }

    public static MethodDescriptor forMethod(Method method) {
        return forMethod(method, DEFAULT_DEPTH);
    }

    private static MethodDescriptor forMethod(Method method, int depth) {
        MethodDescriptor.Builder builder = new MethodDescriptor.Builder(Reflections.getMethodName(method));

        builder.internal(method).returnType(forClass(method.getReturnType(), depth - 1));

        for (Parameter parameter : method.getParameters()) {
            builder.parameters(forParameter(parameter, depth - 1));
        }

        for (Class<?> exceptionType : method.getExceptionTypes()) {
            builder.exceptionTypes(forClass(exceptionType, depth - 1));
        }

        for (Annotation annotation : method.getAnnotations()) {
            builder.annotation(forAnnotation(annotation, depth - 1));
        }

        return builder.build();
    }

    public static FieldDescriptor forField(Field field) {
        return forField(field, DEFAULT_DEPTH);
    }

    private static FieldDescriptor forField(Field field, int depth) {
        FieldDescriptor.
        return null; // todo: not implemented yet
    }

    public static ConstructorDescriptor forConstructor(Constructor<?> constructor) {
        return forConstructor(constructor, DEFAULT_DEPTH);
    }

    private static ConstructorDescriptor forConstructor(Constructor<?> constructor, int depth) {
        return null; // todo: not implemented yet
    }

    public static ClassDescriptor forClass(Class<?> type) {
        return forClass(type, DEFAULT_DEPTH);
    }

    private static ClassDescriptor forClass(Class<?> type, int depth) {
        ClassDescriptor.Builder builder = new ClassDescriptor.Builder(getShortName(type));

        // DETECTOR.detect(() -> type, CYCLIC_REFERENCE_EXCEPTION);

        builder.internal(type);

        if (depth > 0) {
            for (Annotation annotation : type.getAnnotations()) {
                builder.annotation(forAnnotation(annotation, depth - 1));
            }

            for (Method method : type.getDeclaredMethods()) {
                builder.method(forMethod(method, depth - 1));
            }

            for (Constructor<?> ctor : type.getDeclaredConstructors()) {
                builder.constructor(forConstructor(ctor, depth - 1));
            }

            for (Field field : type.getDeclaredFields()) {
                builder.field(forField(field, depth - 1));
            }
        }

        DETECTOR.remove(() -> type);

        return builder.build();
    }

    public static AnnotationDescriptor forAnnotation(Annotation annotation) {
        return forAnnotation(annotation, DEFAULT_DEPTH);
    }

    public static AnnotationDescriptor forAnnotation(Annotation annotation, int depth) {
        Class<? extends Annotation>  type    = annotation.annotationType();
        AnnotationDescriptor.Builder builder = new AnnotationDescriptor.Builder(getShortName(type));

        builder.internal(annotation).annotationType(forClass(type, depth - 1));

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
