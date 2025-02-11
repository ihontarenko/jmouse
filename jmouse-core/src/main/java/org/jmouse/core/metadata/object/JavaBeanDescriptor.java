package org.jmouse.core.metadata.object;

import org.jmouse.core.metadata.*;

import java.util.*;

public interface JavaBeanDescriptor<T> extends ObjectDescriptor<T> {

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
     * Default implementation of {@link JavaBeanDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing metadata related to beans,
     * including their properties, methods, constructors, and annotations.
     * </p>
     * <p>
     * During construction, each {@link JavaBeanPropertyDescriptor} is assigned an owner reference
     * to this bean descriptor, ensuring proper linkage.
     * </p>
     *
     * @param <T> the type of the bean
     */
    class Implementation<T> extends ObjectDescriptor.Implementation<T> implements JavaBeanDescriptor<T> {

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
            super(name, internal, annotations, type, properties);
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
            return "[%s] : %s".formatted(type.isRecord() ? "VO" : "JB", super.toString());
        }
    }

    class Builder<T> extends ObjectDescriptor.Builder<T> {

        public Builder(String name) {
            super(name);
        }

        @Override
        public JavaBeanDescriptor<T> build() {
            return new Implementation<>(
                    name,
                    internal,
                    Collections.unmodifiableSet(annotations),
                    descriptor,
                    Collections.unmodifiableMap(properties)
            );
        }
    }
}
