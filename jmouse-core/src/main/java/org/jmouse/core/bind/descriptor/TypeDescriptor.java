package org.jmouse.core.bind.descriptor;

import org.jmouse.core.reflection.JavaType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * Represents a descriptor for a type, providing descriptor about the associated Java class.
 * <p>
 * This interface extends {@link ElementDescriptor} and {@link ClassTypeInspector},
 * allowing introspection of the underlying class and its type information.
 * </p>
 *
 * @see ElementDescriptor
 * @see ClassTypeInspector
 */
public interface TypeDescriptor extends ElementDescriptor<Class<?>>, ClassTypeInspector {

    int                           DEFAULT_DEPTH = 4;
    Map<Class<?>, TypeDescriptor> CACHE         = new HashMap<>();

    /**
     * Returns the {@link JavaType} representation of this type.
     * <p>
     * This method provides additional type descriptor, including generics and hierarchy details.
     * </p>
     *
     * @return the {@link JavaType} representing this type
     */
    JavaType getType();

    /**
     * Returns the raw Java class associated with this type.
     * <p>
     * This method retrieves the underlying raw class, stripping away any generic type information.
     * It is equivalent to calling {@code getType().getRawType()}.
     * </p>
     *
     * @return the raw {@link Class} bean representing this type
     */
    default Class<?> getRawType() {
        return getType().getRawType();
    }

    /**
     * Returns a collection of method descriptors associated with this class.
     *
     * @return a collection of {@link MethodDescriptor} instances
     */
    Collection<MethodDescriptor> getMethods();

    /**
     * Retrieves a method descriptor by name.
     *
     * @param name the name of the method
     * @return the corresponding {@link MethodDescriptor}, or {@code null} if not found
     */
    MethodDescriptor getMethod(String name);

    /**
     * Checks if this class contains a method with the given name.
     *
     * @param name the name of the method
     * @return {@code true} if the method exists, otherwise {@code false}
     */
    boolean hasMethod(String name);

    /**
     * Returns a collection of constructor descriptors associated with this class.
     *
     * @return a collection of {@link ConstructorDescriptor} instances
     */
    Collection<ConstructorDescriptor> getConstructors();

    /**
     * Returns a collection of field descriptors associated with this class.
     *
     * @return a collection of {@link FieldDescriptor} instances
     */
    Collection<FieldDescriptor> getFields();

    /**
     * Retrieves a field descriptor by name.
     *
     * @param name the name of the field
     * @return the corresponding {@link FieldDescriptor}, or {@code null} if not found
     */
    FieldDescriptor getField(String name);

    /**
     * Checks if this class contains a field with the given name.
     *
     * @param name the name of the field
     * @return {@code true} if the field exists, otherwise {@code false}
     */
    boolean hasField(String name);

    /**
     * Default implementation of {@link TypeDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing descriptor related to classes,
     * including their methods, fields, constructors, and annotations.
     * </p>
     */
    class Implementation extends ElementDescriptor.Implementation<Class<?>> implements TypeDescriptor {

        private final Collection<ConstructorDescriptor> constructors;
        private final Map<String, FieldDescriptor>      fields;
        private final Map<String, MethodDescriptor>     methods;
        private final JavaType                          javaType;

        /**
         * Constructs a new {@code ClassDescriptor.Implementation} instance.
         *
         * @param name         the name of the class
         * @param internal     the underlying {@link Class} instance
         * @param annotations  a set of annotation descriptors associated with this class
         * @param classType    the actual class type
         * @param constructors a collection of constructor descriptors
         * @param fields       a map of field names to field descriptors
         * @param methods      a map of method names to method descriptors
         */
        Implementation(
                String name, Class<?> internal,
                Set<AnnotationDescriptor> annotations,
                Class<?> classType,
                Collection<ConstructorDescriptor> constructors,
                Map<String, FieldDescriptor> fields,
                Map<String, MethodDescriptor> methods
        ) {
            super(name, internal, annotations);
            this.constructors = constructors;
            this.fields = fields;
            this.methods = methods;
            this.javaType = JavaType.forType(classType);
        }

        /**
         * Returns the class type being described.
         *
         * @return the {@link Class} bean representing the described class
         */
        @Override
        public Class<?> getClassType() {
            return internal;
        }

        /**
         * Returns the {@link JavaType} representation of this type.
         * <p>
         * This method provides additional type descriptor, including generics and hierarchy details.
         * </p>
         *
         * @return the {@link JavaType} representing this type
         */
        @Override
        public JavaType getType() {
            return javaType;
        }

        /**
         * Returns a collection of method descriptors associated with this class.
         *
         * @return a collection of {@link MethodDescriptor} instances
         */
        @Override
        public Collection<MethodDescriptor> getMethods() {
            return methods.values();
        }

        /**
         * Checks if this class contains a method with the given name.
         *
         * @param name the name of the method
         * @return {@code true} if the method exists, otherwise {@code false}
         */
        @Override
        public boolean hasMethod(String name) {
            return methods.containsKey(name);
        }

        /**
         * Retrieves a method descriptor by name.
         *
         * @param name the name of the method
         * @return the corresponding {@link MethodDescriptor}, or {@code null} if not found
         */
        @Override
        public MethodDescriptor getMethod(String name) {
            return methods.get(name);
        }

        /**
         * Returns a collection of constructor descriptors associated with this class.
         *
         * @return a collection of {@link ConstructorDescriptor} instances
         */
        @Override
        public Collection<ConstructorDescriptor> getConstructors() {
            return constructors;
        }

        /**
         * Returns a collection of field descriptors associated with this class.
         *
         * @return a collection of {@link FieldDescriptor} instances
         */
        @Override
        public Collection<FieldDescriptor> getFields() {
            return fields.values();
        }

        /**
         * Retrieves a field descriptor by name.
         *
         * @param name the name of the field
         * @return the corresponding {@link FieldDescriptor}, or {@code null} if not found
         */
        @Override
        public FieldDescriptor getField(String name) {
            return fields.get(name);
        }

        /**
         * Checks if this class contains a field with the given name.
         *
         * @param name the name of the field
         * @return {@code true} if the field exists, otherwise {@code false}
         */
        @Override
        public boolean hasField(String name) {
            return fields.containsKey(name);
        }

        /**
         * Returns a string representation of this class descriptor.
         * <p>
         * The format used is the short name of the class.
         * </p>
         *
         * @return a string representation of this class descriptor
         */
        @Override
        public String toString() {
            return getType().toString();
        }
    }

    /**
     * A builder for constructing instances of {@link TypeDescriptor}.
     * <p>
     * This builder provides a fluent API for setting class descriptor before
     * creating an immutable {@link TypeDescriptor} instance.
     * </p>
     */
    class Builder extends ElementDescriptor.Builder<Builder, Class<?>, TypeDescriptor> {

        private final Set<ConstructorDescriptor>    constructors = new HashSet<>();
        private final Map<String, FieldDescriptor>  fields       = new HashMap<>();
        private final Map<String, MethodDescriptor> methods      = new HashMap<>();

        /**
         * Constructs a new {@code ClassDescriptor.Builder} with the specified class name.
         *
         * @param name the name of the class
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Adds a constructor descriptor to this class.
         *
         * @param constructor the constructor descriptor to add
         * @return this builder instance for method chaining
         */
        public Builder constructor(ConstructorDescriptor constructor) {
            constructors.add(constructor);
            return self();
        }

        /**
         * Adds a field descriptor to this class.
         *
         * @param field the field descriptor to add
         * @return this builder instance for method chaining
         */
        public Builder field(FieldDescriptor field) {
            fields.put(field.getName(), field);
            return self();
        }

        /**
         * Adds a method descriptor to this class.
         *
         * @param method the method descriptor to add
         * @return this builder instance for method chaining
         */
        public Builder method(MethodDescriptor method) {
            methods.put(method.getName(), method);
            return self();
        }

        /**
         * Builds a new {@link TypeDescriptor} instance using the configured values.
         *
         * @return a new immutable instance of {@link TypeDescriptor}
         */
        @Override
        public TypeDescriptor build() {
            return new Implementation(
                    name,
                    internal,
                    Collections.unmodifiableSet(annotations),
                    internal,
                    Collections.unmodifiableSet(constructors),
                    Collections.unmodifiableMap(fields),
                    Collections.unmodifiableMap(methods)
            );
        }
    }

    /**
     * Creates a descriptor for a class.
     * <p>
     * If the class descriptor has already been created, it is retrieved from an internal cache.
     * </p>
     *
     * @param type the class to describe
     * @return a {@link TypeDescriptor} instance representing the class
     */
    static TypeDescriptor forClass(Class<?> type) {
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
     * @param depth the recursion depth limit for nested descriptor resolution
     * @return a {@link TypeDescriptor} instance
     */
    static TypeDescriptor forClass(Class<?> type, int depth) {
        if (CACHE.containsKey(type)) {
            return CACHE.get(type);
        }

        TypeDescriptor.Builder builder = new TypeDescriptor.Builder(getShortName(type));
        builder.internal(type);

        if (depth > 0) {
            for (Annotation annotation : type.getAnnotations()) {
                builder.annotation(AnnotationDescriptor.forAnnotation(annotation, depth - 1));
            }

            for (Method method : type.getDeclaredMethods()) {
                builder.method(MethodDescriptor.forMethod(method, depth - 1));
            }

            for (Constructor<?> ctor : type.getDeclaredConstructors()) {
                builder.constructor(ConstructorDescriptor.forContructor(ctor, depth - 1));
            }

            for (Field field : type.getDeclaredFields()) {
                builder.field(FieldDescriptor.forField(field, depth - 1));
            }
        }

        TypeDescriptor descriptor = builder.build();

        CACHE.put(type, descriptor);

        return descriptor;
    }
}
