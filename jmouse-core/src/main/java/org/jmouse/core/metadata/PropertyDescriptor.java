package org.jmouse.core.metadata;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a descriptor for a JavaBean property.
 * <p>
 * This interface extends {@link ElementDescriptor} and {@link ClassTypeInspector},
 * providing metadata about a property, including its type, getter, setter, and owning bean.
 * </p>
 *
 * @param <T> the type of the bean that owns this property
 * @see BeanDescriptor
 * @see MethodDescriptor
 * @see ClassDescriptor
 */
public interface PropertyDescriptor<T> extends ElementDescriptor<T>, ClassTypeInspector {

    /**
     * Returns the type descriptor of this property.
     *
     * @return the {@link ClassDescriptor} representing the property type
     */
    ClassDescriptor getType();

    /**
     * Returns the {@link BeanDescriptor} that owns this property.
     *
     * @return the owning {@link BeanDescriptor}
     */
    BeanDescriptor<T> getOwner();

    /**
     * Sets the owner of this property.
     *
     * @param owner the {@link BeanDescriptor} that owns this property
     */
    void setOwner(BeanDescriptor<T> owner);

    /**
     * Returns the method descriptor representing the getter of this property.
     *
     * @return the {@link MethodDescriptor} for the getter, or {@code null} if not present
     */
    MethodDescriptor getGetter();

    /**
     * Returns the method descriptor representing the setter of this property.
     *
     * @return the {@link MethodDescriptor} for the setter, or {@code null} if not present
     */
    MethodDescriptor getSetter();

    /**
     * Checks if this property has a writable setter method.
     *
     * @return {@code true} if a setter is present, otherwise {@code false}
     */
    default boolean isWritable() {
        return getSetter() != null;
    }

    /**
     * Checks if this property has a readable getter method.
     *
     * @return {@code true} if a getter is present, otherwise {@code false}
     */
    default boolean isReadable() {
        return getGetter() != null;
    }

    /**
     * Default implementation of {@link PropertyDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing metadata related to properties,
     * including their getter, setter, and associated type.
     * </p>
     *
     * @param <T> the type of the bean that owns this property
     */
    class Implementation<T> extends ElementDescriptor.Implementation<T> implements PropertyDescriptor<T> {

        private final MethodDescriptor getter;
        private final MethodDescriptor setter;
        private BeanDescriptor<T> owner;

        /**
         * Constructs a new {@code PropertyDescriptor.Implementation} instance.
         *
         * @param name        the name of the property
         * @param internal    the internal representation of the property
         * @param annotations a set of annotation descriptors associated with this property
         * @param getter      the getter method descriptor (if applicable)
         * @param setter      the setter method descriptor (if applicable)
         * @param owner       the owning {@link BeanDescriptor}
         */
        Implementation(
                String name, T internal,
                Set<AnnotationDescriptor> annotations,
                MethodDescriptor getter,
                MethodDescriptor setter,
                BeanDescriptor<T> owner
        ) {
            super(name, internal, annotations);
            this.getter = getter;
            this.setter = setter;
            this.owner = owner;
        }

        /**
         * Returns the type descriptor of this property.
         *
         * @return the {@link ClassDescriptor} representing the property type
         */
        @Override
        public ClassDescriptor getType() {
            if (isReadable()) {
                return getGetter().getReturnType();
            } else if (isWritable()) {
                return getSetter().getParameters().iterator().next().getType();
            }
            return null;
        }

        /**
         * Returns the {@link BeanDescriptor} that owns this property.
         *
         * @return the owning {@link BeanDescriptor}
         */
        @Override
        public BeanDescriptor<T> getOwner() {
            return owner;
        }

        /**
         * Sets the owner of this property.
         *
         * @param owner the {@link BeanDescriptor} that owns this property
         */
        @Override
        public void setOwner(BeanDescriptor<T> owner) {
            this.owner = owner;
        }

        /**
         * Returns the method descriptor representing the getter of this property.
         *
         * @return the {@link MethodDescriptor} for the getter, or {@code null} if not present
         */
        @Override
        public MethodDescriptor getGetter() {
            return getter;
        }

        /**
         * Returns the method descriptor representing the setter of this property.
         *
         * @return the {@link MethodDescriptor} for the setter, or {@code null} if not present
         */
        @Override
        public MethodDescriptor getSetter() {
            return setter;
        }

        /**
         * Returns the class type of this property.
         *
         * @return the {@link Class} representing the property type
         */
        @Override
        public Class<?> getClassType() {
            return getType() != null ? getType().getClassType() : null;
        }

        /**
         * Returns a string representation of this property descriptor.
         *
         * @return a string representation of this property descriptor
         */
        @Override
        public String toString() {
            return "%s#%s : %s".formatted(getOwner(), getName(), getType());
        }
    }

    /**
     * A builder for constructing instances of {@link PropertyDescriptor}.
     * <p>
     * This builder provides a fluent API for setting property metadata before
     * creating an immutable {@link PropertyDescriptor} instance.
     * </p>
     *
     * @param <T> the type of the bean that owns this property
     */
    class Builder<T> extends ElementDescriptor.Builder<Builder<T>, T, PropertyDescriptor<T>> {

        private MethodDescriptor getter;
        private MethodDescriptor setter;
        private BeanDescriptor<T> owner;

        /**
         * Constructs a new {@code PropertyDescriptor.Builder} with the specified property name.
         *
         * @param name the name of the property
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the getter method descriptor for this property.
         *
         * @param getter the {@link MethodDescriptor} representing the getter
         * @return this builder instance for method chaining
         */
        public Builder<T> getter(MethodDescriptor getter) {
            this.getter = getter;
            return self();
        }

        /**
         * Sets the setter method descriptor for this property.
         *
         * @param setter the {@link MethodDescriptor} representing the setter
         * @return this builder instance for method chaining
         */
        public Builder<T> setter(MethodDescriptor setter) {
            this.setter = setter;
            return self();
        }

        /**
         * Sets the owner of this property.
         *
         * @param owner the {@link BeanDescriptor} that owns this property
         * @return this builder instance for method chaining
         */
        public Builder<T> owner(BeanDescriptor<T> owner) {
            this.owner = owner;
            return self();
        }

        /**
         * Builds a new {@link PropertyDescriptor} instance using the configured values.
         *
         * @return a new immutable instance of {@link PropertyDescriptor}
         */
        @Override
        public PropertyDescriptor<T> build() {
            return new Implementation<>(
                    name,
                    internal,
                    Collections.unmodifiableSet(annotations),
                    getter,
                    setter,
                    owner
            );
        }
    }
}
