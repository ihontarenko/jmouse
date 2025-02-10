package org.jmouse.core.metadata;

import java.util.*;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * Represents a descriptor for a class, providing metadata such as methods, fields,
 * constructors, and annotations.
 * <p>
 * This interface extends {@link ElementDescriptor} and {@link ClassTypeInspector}
 * to allow for detailed analysis of a class and its members.
 * </p>
 *
 * @see ElementDescriptor
 * @see ClassTypeInspector
 * @see MethodDescriptor
 * @see FieldDescriptor
 * @see ConstructorDescriptor
 * @see AnnotationDescriptor
 */
public interface ClassDescriptor extends ElementDescriptor<Class<?>>, ClassTypeInspector {

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
     * Default implementation of {@link ClassDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing metadata related to classes,
     * including their methods, fields, constructors, and annotations.
     * </p>
     */
    class Implementation extends ElementDescriptor.Implementation<Class<?>> implements ClassDescriptor {

        private final Collection<ConstructorDescriptor> constructors;
        private final Map<String, FieldDescriptor>      fields;
        private final Map<String, MethodDescriptor>     methods;

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
        }

        /**
         * Returns the class type being described.
         *
         * @return the {@link Class} object representing the described class
         */
        @Override
        public Class<?> getClassType() {
            return internal;
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
            return "%s".formatted(getShortName(internal));
        }
    }

    /**
     * A builder for constructing instances of {@link ClassDescriptor}.
     * <p>
     * This builder provides a fluent API for setting class metadata before
     * creating an immutable {@link ClassDescriptor} instance.
     * </p>
     */
    class Builder extends ElementDescriptor.Builder<Builder, Class<?>, ClassDescriptor> {

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
         * Builds a new {@link ClassDescriptor} instance using the configured values.
         *
         * @return a new immutable instance of {@link ClassDescriptor}
         */
        @Override
        public ClassDescriptor build() {
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
}
