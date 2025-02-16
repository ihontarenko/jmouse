package org.jmouse.core.bind.descriptor;

import org.jmouse.core.reflection.ClassTypeInspector;
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

    int                           DEFAULT_DEPTH = 2;
    Map<JavaType, TypeDescriptor> CACHE         = new HashMap<>();

    static Mutable builder(JavaType javaType) {
        return new Mutable(javaType);
    }

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
     * Returns a constructor descriptor associated with this class.
     *
     * @return a {@link ConstructorDescriptor} instance
     */
    default ConstructorDescriptor getConstructor(int index) {
        ConstructorDescriptor descriptor = null;

        if (index >= 0 && index < getConstructors().size()) {
            descriptor = List.copyOf(getConstructors()).get(index);
        }

        return descriptor;
    }

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

        private final List<ConstructorDescriptor>   constructors;
        private final Map<String, FieldDescriptor>  fields;
        private final Map<String, MethodDescriptor> methods;
        private final JavaType                      javaType;

        /**
         * Constructs a new {@code ClassDescriptor.PropertyDescriptorAccessor} instance.
         *
         * @param name         the name of the class
         * @param internal     the underlying {@link Class} instance
         * @param annotations  a set of annotation descriptors associated with this class
         * @param type         the actual class type
         * @param constructors a collection of constructor descriptors
         * @param fields       a map of field names to field descriptors
         * @param methods      a map of method names to method descriptors
         */
        Implementation(
                String name,
                Class<?> internal,
                Set<AnnotationDescriptor> annotations,
                JavaType type,
                List<ConstructorDescriptor> constructors,
                Map<String, FieldDescriptor> fields,
                Map<String, MethodDescriptor> methods
        ) {
            super(name, internal, annotations);
            this.constructors = constructors;
            this.fields = fields;
            this.methods = methods;
            this.javaType = type;
        }

        @Override
        public TypeDescriptor introspect() {
            return null;
        }

        /**
         * Returns the class type being described.
         *
         * @return the {@link Class} bean representing the described class
         */
        @Override
        public Class<?> getClassType() {
            return target;
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
    class Mutable extends ElementDescriptor.Mutable<Mutable, Class<?>, TypeDescriptor> {

        private final Map<String, FieldDescriptor>   fields       = new HashMap<>();
        private final List<ConstructorDescriptor>    constructors = new ArrayList<>();
        private final Map<String, MethodDescriptor>  methods      = new HashMap<>();
        private       JavaType                       type;

        /**
         * Constructs a new {@code ClassDescriptor.Builder} with the specified class name.
         *
         * @param classType the name of the class
         */
        public Mutable(JavaType javaType) {
            super(javaType.getClassType());
        }

        /**
         * Adds a constructor descriptor to this class.
         *
         * @param constructor the constructor descriptor to add
         * @return this builder instance for method chaining
         */
        public Mutable constructor(ConstructorDescriptor constructor) {
            constructors.add(constructor);
            return self();
        }

        /**
         * Adds a field descriptor to this class.
         *
         * @param field the field descriptor to add
         * @return this builder instance for method chaining
         */
        public Mutable field(FieldDescriptor field) {
            fields.put(field.getName(), field);
            return self();
        }

        /**
         * Adds a method descriptor to this class.
         * <p>
         * This method registers a {@link MethodDescriptor} representing a method in the class.
         * The method is stored in a map using its name as the key.
         * </p>
         *
         * @param method the method descriptor to add
         * @return this builder instance for method chaining
         */
        public Mutable method(MethodDescriptor method) {
            methods.put(method.getName(), method);
            return self();
        }

        /**
         * Sets the {@link JavaType} of this class.
         * <p>
         * This method defines the Java type that the class descriptor represents.
         * </p>
         *
         * @param type the Java type of the class
         * @return this builder instance for method chaining
         */
        public Mutable type(JavaType type) {
            this.type = type;
            return self();
        }

        /**
         * Builds a new {@link TypeDescriptor} instance using the configured values.
         *
         * @return a new immutable instance of {@link TypeDescriptor}
         */
        @Override
        public TypeDescriptor toImmutable() {
            return new Implementation(
                    name,
                    target,
                    Collections.unmodifiableSet(annotations),
                    type,
                    Collections.unmodifiableList(constructors),
                    Collections.unmodifiableMap(fields),
                    Collections.unmodifiableMap(methods)
            );
        }
    }

    static TypeDescriptor forType(JavaType type) {
        return forType(type, DEFAULT_DEPTH);
    }

    static TypeDescriptor forClass(Class<?> type) {
        return forType(JavaType.forType(type), DEFAULT_DEPTH);
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
    static TypeDescriptor forType(JavaType type, int depth) {
        TypeDescriptor descriptor = CACHE.get(type);

        if (descriptor == null) {
            Class<?> rawType = type.getRawType();
            Mutable  builder = new Mutable(type.getClassType());

            builder.target(rawType).type(type);

            if (depth > 0) {
                for (Annotation annotation : rawType.getAnnotations()) {
                    builder.annotation(AnnotationDescriptor.forAnnotation(annotation, depth - 1));
                }

                for (Method method : rawType.getDeclaredMethods()) {
                    builder.method(MethodDescriptor.forMethod(method, depth - 1));
                }

                for (Constructor<?> ctor : rawType.getDeclaredConstructors()) {
                    builder.constructor(ConstructorDescriptor.forContructor(ctor, depth - 1));
                }

                for (Field field : rawType.getDeclaredFields()) {
                    builder.field(FieldDescriptor.forField(field, depth - 1));
                }
            }

            descriptor = builder.toImmutable();

            CACHE.put(type, descriptor);
        }

        return descriptor;
    }
}
