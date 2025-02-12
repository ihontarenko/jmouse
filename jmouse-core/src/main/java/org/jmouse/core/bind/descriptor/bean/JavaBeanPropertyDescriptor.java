package org.jmouse.core.bind.descriptor.bean;

import org.jmouse.core.bind.descriptor.AnnotationDescriptor;
import org.jmouse.core.bind.descriptor.MethodDescriptor;
import org.jmouse.core.bind.descriptor.TypeDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a JavaBean-style property descriptor, including descriptor about
 * its getter and setter methods.
 * <p>
 * This interface extends {@link PropertyDescriptor} and provides additional methods
 * to retrieve information about the getter and setter methods associated with the property.
 * </p>
 *
 * @param <T> the type of the bean containing the property
 */
public interface JavaBeanPropertyDescriptor<T> extends PropertyDescriptor<T> {

    /**
     * Returns the method descriptor for the getter method of this property.
     */
    MethodDescriptor getGetterMethod();

    /**
     * Returns the method descriptor for the setter method of this property.
     */
    MethodDescriptor getSetterMethod();

    /**
     * A default implementation of {@link JavaBeanPropertyDescriptor}.
     * <p>
     * This implementation extends {@link PropertyDescriptor.Implementation} and
     * includes references to getter and setter method descriptors.
     * </p>
     *
     * @param <T> the type of the bean containing the property
     */
    class Implementation<T> extends PropertyDescriptor.Implementation<T> implements JavaBeanPropertyDescriptor<T> {

        private final MethodDescriptor getterMethod;
        private final MethodDescriptor setterMethod;

        /**
         * Constructs a new {@code JavaBeanPropertyDescriptor.Implementation}.
         *
         * @param name        the name of the property
         * @param internal    the internal representation of the property
         * @param annotations the annotations associated with this property
         * @param getter      the getter function for retrieving property values
         * @param setter      the setter function for modifying property values
         * @param getterMethod the method descriptor representing the getter method
         * @param setterMethod the method descriptor representing the setter method
         * @param owner       the owner descriptor that contains this property
         */
        Implementation(
                String name,
                T internal,
                Set<AnnotationDescriptor> annotations,
                Getter<T, Object> getter,
                Setter<T, Object> setter,
                MethodDescriptor getterMethod,
                MethodDescriptor setterMethod,
                ObjectDescriptor<T> owner
        ) {
            super(name, internal, annotations, getter, setter, owner);

            this.getterMethod = getterMethod;
            this.setterMethod = setterMethod;
        }

        /**
         * Returns the class descriptor representing the type of this property.
         *
         * @return the {@link TypeDescriptor} representing the property's type
         */
        @Override
        public TypeDescriptor getType() {
            if (isReadable()) {
                return getGetterMethod().getReturnType();
            } else if (isWritable()) {
                return getSetterMethod().getParameters().iterator().next().getType();
            }
            return null;
        }

        /**
         * Returns the method descriptor for the setter method of this property.
         */
        @Override
        public MethodDescriptor getGetterMethod() {
            return getterMethod;
        }

        /**
         * Returns the method descriptor for the setter method of this property.
         */
        @Override
        public MethodDescriptor getSetterMethod() {
            return setterMethod;
        }

        /**
         * Returns the class type of this property.
         *
         * @return the {@link Class} bean representing the property type, or {@code null} if not available
         */
        @Override
        public Class<?> getClassType() {
            return getType() != null ? getType().getClassType() : null;
        }

    }

    /**
     * A builder class for constructing {@link JavaBeanPropertyDescriptor} instances.
     *
     * @param <T> the type of the bean containing the property
     */
    class Builder<T> extends PropertyDescriptor.Builder<Builder<T>, T, JavaBeanPropertyDescriptor<T>> {

        private MethodDescriptor getterMethod;
        private MethodDescriptor setterMethod;

        /**
         * Constructs a new {@code JavaBeanPropertyDescriptor.Builder}.
         *
         * @param name the name of the property being built
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the getter method descriptor for this property.
         *
         * @param getter the getter method descriptor
         * @return this builder instance
         */
        public Builder<T> getterMethod(MethodDescriptor getter) {
            this.getterMethod = getter;
            return self();
        }

        /**
         * Sets the setter method descriptor for this property.
         *
         * @param setter the setter method descriptor
         * @return this builder instance
         */
        public Builder<T> setterMethod(MethodDescriptor setter) {
            this.setterMethod = setter;
            return self();
        }

        /**
         * Sets the owner descriptor for this property.
         *
         * @param owner the {@link JavaBeanDescriptor} representing the owner
         * @return this builder instance
         */
        public Builder<T> owner(JavaBeanDescriptor<T> owner) {
            this.owner = owner;
            return self();
        }

        /**
         * Builds and returns a {@link JavaBeanPropertyDescriptor} instance.
         *
         * @return a new instance of {@code JavaBeanPropertyDescriptor}
         */
        @Override
        public JavaBeanPropertyDescriptor<T> build() {
            return new Implementation<>(
                    name,
                    internal,
                    Collections.unmodifiableSet(annotations),
                    getter,
                    setter,
                    getterMethod,
                    setterMethod,
                    owner
            );
        }
    }
}
