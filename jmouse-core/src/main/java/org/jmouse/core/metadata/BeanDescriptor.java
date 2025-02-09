package org.jmouse.core.metadata;

import java.util.*;

public interface BeanDescriptor<T> extends ElementDescriptor<T>, ClassTypeInspector {

    Collection<PropertyDescriptor<T>> getProperties();

    PropertyDescriptor<T> getProperty(String name);

    boolean hasProperty(String name);

    ClassDescriptor getBeanClass();

    Collection<MethodDescriptor> getMethods();

    Collection<ConstructorDescriptor> getConstructors();

    Collection<FieldDescriptor> getFields();

    class Implementation<T> extends ElementDescriptor.Implementation<T> implements BeanDescriptor<T> {

        private final ClassDescriptor                    type;
        private final Map<String, PropertyDescriptor<T>> properties;

        Implementation(
                String name, T internal,
                Set<AnnotationDescriptor> annotations,
                ClassDescriptor type,
                Map<String, PropertyDescriptor<T>> properties
        ) {
            super(name, internal, annotations);
            this.type = type;
            this.properties = properties;
        }

        /**
         * Returns the class type being inspected.
         *
         * @return the {@link Class} object representing the inspected type
         */
        @Override
        public Class<?> getClassType() {
            return getBeanClass().getClassType();
        }

        @Override
        public Collection<PropertyDescriptor<T>> getProperties() {
            return properties.values();
        }

        @Override
        public PropertyDescriptor<T> getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public boolean hasProperty(String name) {
            return properties.containsKey(name);
        }

        @Override
        public ClassDescriptor getBeanClass() {
            return type;
        }

        @Override
        public Collection<MethodDescriptor> getMethods() {
            return getBeanClass().getMethods();
        }

        @Override
        public Collection<ConstructorDescriptor> getConstructors() {
            return getBeanClass().getConstructors();
        }

        @Override
        public Collection<FieldDescriptor> getFields() {
            return getBeanClass().getFields();
        }

        @Override
        public String toString() {
            return "Bean: %s".formatted(type);
        }
    }

    class Builder<T> extends ElementDescriptor.Builder<Builder<T>, T, BeanDescriptor<T>> {

        private Map<String, PropertyDescriptor<T>> properties = new LinkedHashMap<>();
        private T                                  bean;
        private ClassDescriptor                    descriptor;

        /**
         * Constructs a new {@code ElementDescriptor.Builder} with the given name.
         *
         * @param name the name of the element being built
         */
        public Builder(String name) {
            super(name);
        }

        public Builder<T> bean(T bean) {
            this.bean = bean;
            return internal(bean).self();
        }

        public Builder<T> descriptor(ClassDescriptor descriptor) {
            this.descriptor = descriptor;
            return self();
        }

        public Builder<T> properties(Map<String, PropertyDescriptor<T>> properties) {
            this.properties = properties;
            return self();
        }

        public Builder<T> property(PropertyDescriptor<T> property) {
            this.properties.put(property.getName(), property);
            return self();
        }

        /**
         * Constructs the final descriptor instance.
         *
         * @return a new instance of {@code D}
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
