package org.jmouse.core.metadata;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.metadata.PropertyDescriptor.Builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.jmouse.core.reflection.MethodMatchers.*;
import static org.jmouse.core.reflection.Reflections.getPropertyName;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A utility class for creating metadata descriptors for Java reflection elements.
 * <p>
 * This class provides static methods to generate {@link Descriptor} implementations
 * for classes, methods, constructors, fields, parameters, and annotations.
 * It also caches class descriptors to optimize repeated lookups.
 * </p>
 *
 * @see ClassDescriptor
 * @see MethodDescriptor
 * @see ConstructorDescriptor
 * @see FieldDescriptor
 * @see ParameterDescriptor
 * @see AnnotationDescriptor
 */
public final class MetaDescriptor {

    private static final int                            DEFAULT_DEPTH = 4;
    private static final Map<Class<?>, ClassDescriptor> CACHE         = new HashMap<>();

    private MetaDescriptor() {}

    /**
     * Creates a descriptor for a method parameter.
     *
     * @param parameter the parameter to describe
     * @return a {@link ParameterDescriptor} instance representing the parameter
     */
    public static ParameterDescriptor forParameter(Parameter parameter) {
        return forParameter(parameter, DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a method parameter with a specified depth for nested elements.
     *
     * @param parameter the parameter to describe
     * @param depth     the recursion depth limit for nested metadata resolution
     * @return a {@link ParameterDescriptor} instance
     */
    public static ParameterDescriptor forParameter(Parameter parameter, int depth) {
        ParameterDescriptor.Builder builder = new ParameterDescriptor.Builder(parameter.getName());

        for (Annotation annotation : parameter.getAnnotations()) {
            builder.annotation(forAnnotation(annotation, depth - 1));
        }

        builder.type(forClass(parameter.getType(), depth - 1));
        builder.internal(parameter);

        return builder.build();
    }

    /**
     * Creates a descriptor for a method.
     *
     * @param method the method to describe
     * @return a {@link MethodDescriptor} instance representing the method
     */
    public static MethodDescriptor forMethod(Method method) {
        return forMethod(method, DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a method with a specified depth for nested elements.
     *
     * @param method the method to describe
     * @param depth  the recursion depth limit for nested metadata resolution
     * @return a {@link MethodDescriptor} instance
     */
    public static MethodDescriptor forMethod(Method method, int depth) {
        MethodDescriptor.Builder builder = new MethodDescriptor.Builder(method.getName());

        forExecutable(builder, method, depth);

        return builder.returnType(forClass(method.getReturnType(), depth - 1)).build();
    }

    /**
     * Creates a descriptor for a field.
     *
     * @param field the field to describe
     * @return a {@link FieldDescriptor} instance representing the field
     */
    public static FieldDescriptor forField(Field field) {
        return forField(field, DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a field with a specified depth for nested elements.
     *
     * @param field the field to describe
     * @param depth the recursion depth limit for nested metadata resolution
     * @return a {@link FieldDescriptor} instance
     */
    public static FieldDescriptor forField(Field field, int depth) {
        FieldDescriptor.Builder builder = new FieldDescriptor.Builder(field.getName());

        builder.internal(field).type(forClass(field.getType(), depth - 1));

        for (Annotation annotation : field.getAnnotations()) {
            builder.annotation(forAnnotation(annotation, depth - 1));
        }

        return builder.build();
    }

    /**
     * Creates a descriptor for a constructor.
     *
     * @param constructor the constructor to describe
     * @return a {@link ConstructorDescriptor} instance representing the constructor
     */
    public static ConstructorDescriptor forConstructor(Constructor<?> constructor) {
        return forConstructor(constructor, DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a constructor with a specified depth for nested elements.
     *
     * @param constructor the constructor to describe
     * @param depth       the recursion depth limit for nested metadata resolution
     * @return a {@link ConstructorDescriptor} instance
     */
    public static ConstructorDescriptor forConstructor(Constructor<?> constructor, int depth) {
        ConstructorDescriptor.Builder builder = new ConstructorDescriptor.Builder(constructor.getName());

        forExecutable(builder, constructor, depth);

        return builder.build();
    }

    /**
     * Creates a descriptor for a constructor or method with a specified depth for nested elements.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void forExecutable(ExecutableDescriptor.Builder builder, Executable executable, int depth) {
        builder.internal(executable);

        for (Parameter parameter : executable.getParameters()) {
            builder.parameter(forParameter(parameter, depth - 1));
        }

        for (Class<?> exceptionType : executable.getExceptionTypes()) {
            builder.exceptionType(forClass(exceptionType, depth - 1));
        }

        for (Annotation annotation : executable.getAnnotations()) {
            builder.annotation(forAnnotation(annotation, depth - 1));
        }
    }

    /**
     * Creates a descriptor for a class.
     * <p>
     * If the class descriptor has already been created, it is retrieved from an internal cache.
     * </p>
     *
     * @param type the class to describe
     * @return a {@link ClassDescriptor} instance representing the class
     */
    public static ClassDescriptor forClass(Class<?> type) {
        return forClass(type, DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a class with a specified depth for nested elements.
     * <p>
     * This method retrieves the class descriptor from an internal cache if available.
     * It recursively analyzes the class's fields, methods, constructors, and annotations,
     * depending on the specified depth.
     * </p>
     *
     * @param type  the class to describe
     * @param depth the recursion depth limit for nested metadata resolution
     * @return a {@link ClassDescriptor} instance
     */
    public static ClassDescriptor forClass(Class<?> type, int depth) {
        if (CACHE.containsKey(type)) {
            return CACHE.get(type);
        }

        ClassDescriptor.Builder builder = new ClassDescriptor.Builder(getShortName(type));
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

        ClassDescriptor descriptor = builder.build();

        CACHE.put(type, descriptor);

        return descriptor;
    }

    /**
     * Creates a descriptor for an annotation.
     *
     * @param annotation the annotation to describe
     * @return an {@link AnnotationDescriptor} instance
     */
    public static AnnotationDescriptor forAnnotation(Annotation annotation) {
        return forAnnotation(annotation, DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for an annotation with a specified depth for nested elements.
     * <p>
     * This method extracts metadata about the annotation, including its type and attributes.
     * The depth parameter controls how deep nested annotations and class descriptors are resolved.
     * </p>
     *
     * @param annotation the annotation to describe
     * @param depth      the recursion depth limit for nested metadata resolution
     * @return an {@link AnnotationDescriptor} instance
     */
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
        } catch (Exception ignored) {
        }

        return builder.build();
    }

    /**
     * Creates a descriptor for a JavaBean.
     * <p>
     * This method analyzes the given class and generates a {@link BeanDescriptor},
     * detecting properties based on JavaBean getter/setter conventions.
     * </p>
     *
     * @param type the class to describe
     * @param <T>  the type of the bean
     * @return a {@link BeanDescriptor} instance representing the bean
     */
    public static <T> BeanDescriptor<T> forBean(Class<T> type) {
        return forBean(type, null);
    }

    /**
     * Creates a descriptor for a JavaBean with an optional instance.
     * <p>
     * If an instance is provided, it is stored inside the descriptor for further inspection.
     * The descriptor is constructed based on JavaBean conventions for property detection.
     * </p>
     *
     * @param type the class to describe
     * @param bean an optional instance of the bean
     * @param <T>  the type of the bean
     * @return a {@link BeanDescriptor} instance representing the bean
     */
    public static <T> BeanDescriptor<T> forBean(Class<T> type, T bean) {
        return forBean(type, bean, DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a JavaBean with an optional instance and specified depth.
     * <p>
     * The method scans for getter and setter methods to construct property descriptors.
     * The recursion depth determines how deeply nested types should be resolved.
     * </p>
     *
     * @param type  the class to describe
     * @param bean  an optional instance of the bean
     * @param depth the recursion depth limit for nested metadata resolution
     * @param <T>   the type of the bean
     * @return a {@link BeanDescriptor} instance representing the bean
     */
    public static <T> BeanDescriptor<T> forBean(Class<T> type, T bean, int depth) {
        // Map to store property builders
        Map<String, Builder<T>> builders = new HashMap<>();

        // Initialize the BeanDescriptor builder with the short name of the class
        BeanDescriptor.Builder<T> builder = new BeanDescriptor.Builder<>(getShortName(type));

        // Generate the class descriptor for the given type
        ClassDescriptor typeDescriptor = forClass(type, depth);

        // Define matchers for getter and setter methods
        Matcher<Executable> anyMatcher = getter().or(setter());
        BiConsumer<Builder<T>, MethodDescriptor> getter = Builder::getter;
        BiConsumer<Builder<T>, MethodDescriptor> setter = Builder::setter;

        // Iterate over all methods in the class descriptor
        for (MethodDescriptor method : typeDescriptor.getMethods()) {
            Method rawMethod = method.getInternal();

            // Check if the method matches getter or setter patterns
            if (anyMatcher.matches(rawMethod)) {
                BiConsumer<Builder<T>, MethodDescriptor> adder = (a, b) -> {};
                String name = null;

                // Determine if the method is a getter ("is" or "get" prefix)
                if (getter().matches(rawMethod) && prefixIs().matches(rawMethod)) {
                    name = getPropertyName(rawMethod, GETTER_IS_PREFIX);
                    adder = getter;
                } else if (getter().matches(rawMethod)) {
                    name = getPropertyName(rawMethod, GETTER_GET_PREFIX);
                    adder = getter;
                }
                // Determine if the method is a setter ("set" prefix)
                else if (setter().matches(rawMethod)) {
                    name = getPropertyName(rawMethod, SETTER_PREFIX);
                    adder = setter;
                }

                // Associate the method with a property descriptor
                adder.accept(builders.computeIfAbsent(name, Builder::new), forMethod(rawMethod, depth - 1));

                // Copy annotations from method to property descriptor
                if (builders.containsKey(name)) {
                    method.getAnnotations().forEach(builders.get(name)::annotation);
                }

                // Copy annotations from corresponding field (if exists)
                if (typeDescriptor.hasField(name)) {
                    typeDescriptor.getField(name).getAnnotations().forEach(builders.get(name)::annotation);
                }
            }
        }

        // Assign the class descriptor to the bean descriptor
        builder.descriptor(typeDescriptor);

        // If a bean instance is provided, attach it to the descriptor
        if (bean != null) {
            builder.bean(bean).internal(bean);
        }

        // Build the final BeanDescriptor instance
        BeanDescriptor<T> beanDescriptor = builder.build();

        // Assign properties to the final descriptor
        for (Builder<T> property : builders.values()) {
            builder.property(property.owner(beanDescriptor).build());
        }

        return beanDescriptor;
    }

    /**
     * Creates a descriptor for a record (value object).
     * <p>
     * This method detects record components and generates a {@link BeanDescriptor},
     * treating them as immutable properties.
     * </p>
     *
     * @param type the record class to describe
     * @param <T>  the type of the record
     * @return a {@link BeanDescriptor} instance representing the record
     */
    public static <T extends Record> BeanDescriptor<T> forValueObject(Class<T> type) {
        return forValueObject(type, null);
    }

    /**
     * Creates a descriptor for a record (value object) with an optional instance.
     * <p>
     * This method extracts metadata from record components and generates a descriptor
     * treating them as immutable properties.
     * </p>
     *
     * @param type the record class to describe
     * @param bean an optional instance of the record
     * @param <T>  the type of the record
     * @return a {@link BeanDescriptor} instance representing the record
     */
    public static <T extends Record> BeanDescriptor<T> forValueObject(Class<T> type, T bean) {
        return forValueObject(type, bean, DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a record (value object) with an optional instance and specified depth.
     * <p>
     * Unlike standard JavaBeans, records do not have setters; their components are immutable.
     * This method detects record components and maps them to property descriptors.
     * </p>
     *
     * @param type  the record class to describe
     * @param bean  an optional instance of the record
     * @param depth the recursion depth limit for nested metadata resolution
     * @param <T>   the type of the record
     * @return a {@link BeanDescriptor} instance representing the record
     */
    public static <T extends Record> BeanDescriptor<T> forValueObject(Class<T> type, T bean, int depth) {
        // Initialize the builder for the BeanDescriptor using the class short name
        BeanDescriptor.Builder<T> builder = new BeanDescriptor.Builder<>(getShortName(type));

        // Generate a ClassDescriptor for the given record type
        ClassDescriptor descriptor = forClass(type, depth);

        // Assign the class descriptor to the builder
        builder.descriptor(descriptor);

        // If an instance of the record is provided, attach it to the descriptor
        if (bean != null) {
            builder.bean(bean).internal(bean);
        }

        // Build the BeanDescriptor for the record
        BeanDescriptor<T> beanDescriptor = builder.build();

        // Iterate over all record components (fields implicitly declared in the record)
        for (RecordComponent component : type.getRecordComponents()) {
            // Initialize a property descriptor builder for each record component
            PropertyDescriptor.Builder<T> propertyBuilder = new PropertyDescriptor.Builder<>(component.getName());

            // Link the getter method of the record component
            propertyBuilder.owner(beanDescriptor).getter(forMethod(component.getAccessor(), depth - 1));

            // Add the property to the bean descriptor
            builder.property(propertyBuilder.build());

            // If the field exists in the descriptor, copy its annotations to the property
            if (descriptor.hasField(component.getName())) {
                FieldDescriptor field = descriptor.getField(component.getName());
                field.getAnnotations().forEach(propertyBuilder::annotation);
            }

            // If a method with the same name exists, copy its annotations to the property
            if (descriptor.hasMethod(component.getName())) {
                MethodDescriptor method = descriptor.getMethod(component.getName());
                method.getAnnotations().forEach(propertyBuilder::annotation);
            }
        }

        // Return the BeanDescriptor for the record
        return beanDescriptor;
    }


}
