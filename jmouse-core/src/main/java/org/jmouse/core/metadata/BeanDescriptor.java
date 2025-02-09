package org.jmouse.core.metadata;

import java.util.*;

/**
 * Represents a descriptor for a JavaBean, providing metadata about its properties, methods,
 * constructors, and fields.
 * <p>
 * This interface extends {@link ElementDescriptor} and {@link ClassTypeInspector}, allowing
 * detailed inspection of a JavaBean's structure and metadata.
 * </p>
 *
 * @param <T> the type of the bean
 * @see PropertyDescriptor
 * @see MethodDescriptor
 * @see ConstructorDescriptor
 * @see FieldDescriptor
 * @see ClassDescriptor
 */
public interface BeanDescriptor<T> extends ElementDescriptor<T>, ClassTypeInspector {

    /**
     * Returns a collection of property descriptors associated with this bean.
     *
     * @return a collection of {@link PropertyDescriptor} instances
     */
    Collection<PropertyDescriptor<T>> getProperties();

    /**
     * Retrieves a property descriptor by name.
     *
     * @param name the name of the property
     * @return the corresponding {@link PropertyDescriptor}, or {@code null} if not found
     */
    PropertyDescriptor<T> getProperty(String name);

    /**
     * Checks if this bean contains a property with the given name.
     *
     * @param name the name of the property
     * @return {@code true} if the property exists, otherwise {@code false}
     */
    boolean hasProperty(String name);

    /**
     * Returns the class descriptor representing the bean's type.
     *
     * @return the {@link ClassDescriptor} of the bean
     */
    ClassDescriptor getBeanClass();

    /**
     * Returns a collection of method descriptors associated with this bean.
     *
     * @return a collection of {@link MethodDescriptor} instances
     */
    Collection<MethodDescriptor> getMethods();

    /**
     * Returns a collection of constructor descriptors associated with this bean.
     *
     * @return a collection of {@link ConstructorDescriptor} instances
     */
    Collection<ConstructorDescriptor> getConstructors();

    /**
     * Returns a collection of field descriptors associated with this bean.
     *
     * @return a collection of {@link FieldDescriptor} instances
     */
    Collection<FieldDescriptor> getFields();

    /**
     * Default implementation of {@link BeanDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing metadata related to beans,
     * including their properties, methods, constructors, and annotations.
     * </p>
     * <p>
     * During construction, each {@link PropertyDescriptor} is assigned an owner reference
     * to this bean descriptor, ensuring proper linkage.
     * </p>
     *
     * @param <T> the type of the bean
     */
    class Implementation<T> extends ElementDescriptor.Implementation<T> implements BeanDescriptor<T> {

        private final ClassDescriptor                    type;
        private final Map<String, PropertyDescriptor<T>> properties;

        /**
         * Constructs a new {@code BeanDescriptor.Implementation} instance.
         *
         * @param name        the name of the bean
         * @param internal    the bean instance
         * @param annotations a set of annotation descriptors associated with this bean
         * @param type        the descriptor representing the class of this bean
         * @param properties  a map of property names to property descriptors
         */
        Implementation(
                String name, T internal,
                Set<AnnotationDescriptor> annotations,
                ClassDescriptor type,
                Map<String, PropertyDescriptor<T>> properties
        ) {
            super(name, internal, annotations);
            this.type = type;
            this.properties = properties;

            // Assigns ownership to each property descriptor
            for (PropertyDescriptor<T> property : properties.values()) {
                property.setOwner(this);
            }
        }

        /**
         * Returns the class type being inspected.
         *
         * @return the {@link Class} object representing the bean type
         */
        @Override
        public Class<?> getClassType() {
            return getBeanClass().getClassType();
        }

        /**
         * Returns a collection of property descriptors associated with this bean.
         *
         * @return a collection of {@link PropertyDescriptor} instances
         */
        @Override
        public Collection<PropertyDescriptor<T>> getProperties() {
            return properties.values();
        }

        /**
         * Retrieves a property descriptor by name.
         *
         * @param name the name of the property
         * @return the corresponding {@link PropertyDescriptor}, or {@code null} if not found
         */
        @Override
        public PropertyDescriptor<T> getProperty(String name) {
            return properties.get(name);
        }

        /**
         * Checks if this bean contains a property with the given name.
         *
         * @param name the name of the property
         * @return {@code true} if the property exists, otherwise {@code false}
         */
        @Override
        public boolean hasProperty(String name) {
            return properties.containsKey(name);
        }

        /**
         * Returns the class descriptor representing the bean's type.
         *
         * @return the {@link ClassDescriptor} of the bean
         */
        @Override
        public ClassDescriptor getBeanClass() {
            return type;
        }

        /**
         * Returns a collection of method descriptors associated with this bean.
         *
         * @return a collection of {@link MethodDescriptor} instances
         */
        @Override
        public Collection<MethodDescriptor> getMethods() {
            return getBeanClass().getMethods();
        }

        /**
         * Returns a collection of constructor descriptors associated with this bean.
         *
         * @return a collection of {@link ConstructorDescriptor} instances
         */
        @Override
        public Collection<ConstructorDescriptor> getConstructors() {
            return getBeanClass().getConstructors();
        }

        /**
         * Returns a collection of field descriptors associated with this bean.
         *
         * @return a collection of {@link FieldDescriptor} instances
         */
        @Override
        public Collection<FieldDescriptor> getFields() {
            return getBeanClass().getFields();
        }

        /**
         * Returns a string representation of this bean descriptor.
         *
         * @return a string representation of this bean descriptor
         */
        @Override
        public String toString() {
            return "[%s] : %s".formatted(type.isRecord() ? "VO" : "JB", type);
        }
    }

    /**
     * A builder for constructing instances of {@link BeanDescriptor}.
     * <p>
     * This builder provides a fluent API for setting bean metadata before
     * creating an immutable {@link BeanDescriptor} instance.
     * </p>
     *
     * @param <T> the type of the bean
     */
    class Builder<T> extends ElementDescriptor.Builder<Builder<T>, T, BeanDescriptor<T>> {

        private Map<String, PropertyDescriptor<T>> properties = new LinkedHashMap<>();
        private T                                  bean;
        private ClassDescriptor                    descriptor;

        /**
         * Constructs a new {@code BeanDescriptor.Builder} with the specified bean name.
         *
         * @param name the name of the bean
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the bean instance being described.
         *
         * @param bean the bean instance
         * @return this builder instance for method chaining
         */
        public Builder<T> bean(T bean) {
            this.bean = bean;
            return internal(bean).self();
        }

        /**
         * Sets the class descriptor representing the bean's type.
         *
         * @param descriptor the {@link ClassDescriptor} of the bean
         * @return this builder instance for method chaining
         */
        public Builder<T> descriptor(ClassDescriptor descriptor) {
            this.descriptor = descriptor;
            return self();
        }

        /**
         * Sets the properties of the bean.
         *
         * @param properties a map of property names to property descriptors
         * @return this builder instance for method chaining
         */
        public Builder<T> properties(Map<String, PropertyDescriptor<T>> properties) {
            this.properties = properties;
            return self();
        }

        /**
         * Adds a property descriptor to the bean.
         *
         * @param property the property descriptor to add
         * @return this builder instance for method chaining
         */
        public Builder<T> property(PropertyDescriptor<T> property) {
            this.properties.put(property.getName(), property);
            return self();
        }

        /**
         * Builds a new {@link BeanDescriptor} instance using the configured values.
         *
         * @return a new immutable instance of {@link BeanDescriptor}
         */
        @Override
        public BeanDescriptor<T> build() {
            return new Implementation<>(
                    name, internal,
                    Collections.unmodifiableSet(annotations),
                    descriptor,
                    Collections.unmodifiableMap(properties)
            );
        }
    }
}
