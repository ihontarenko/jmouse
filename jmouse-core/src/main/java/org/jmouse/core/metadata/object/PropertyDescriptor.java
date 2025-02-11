package org.jmouse.core.metadata.object;

import org.jmouse.core.metadata.AnnotationDescriptor;
import org.jmouse.core.metadata.ClassDescriptor;
import org.jmouse.core.metadata.ClassTypeInspector;
import org.jmouse.core.metadata.ElementDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.util.Set;

/**
 * Represents a property descriptor that provides metadata about an object's property.
 * <p>
 * This interface extends {@link ElementDescriptor} and {@link ClassTypeInspector},
 * allowing access to property-specific metadata such as its type, owner, getter, and setter methods.
 * </p>
 *
 * @param <T> the type of the object containing the property
 */
public interface PropertyDescriptor<T> extends ElementDescriptor<T>, ClassTypeInspector {

    /**
     * Returns the {@link PropertyAccessor} for this property.
     * <p>
     * The property accessor provides a mechanism to dynamically retrieve and modify property values.
     * </p>
     *
     * @return the property accessor for this descriptor
     */
    PropertyAccessor<T> getPropertyAccessor();

    /**
     * Returns the class descriptor representing the type of this property.
     *
     * @return the {@link ClassDescriptor} representing the property's type
     */
    ClassDescriptor getType();

    /**
     * Returns the owner of this property.
     * <p>
     * The owner is the object descriptor that contains this property.
     * </p>
     *
     * @return the {@link ObjectDescriptor} representing the owner of this property
     */
    ObjectDescriptor<T> getOwner();

    /**
     * Returns the getter method for this property.
     *
     * @param <V> the type of the value returned by the getter
     * @return the getter function for retrieving property values
     */
    <V> Getter<T, V> getGetter();

    /**
     * Returns the setter method for this property.
     *
     * @param <V> the type of the value accepted by the setter
     * @return the setter function for modifying property values
     */
    <V> Setter<T, V> getSetter();

    /**
     * Determines whether this property is writable.
     *
     * @return {@code true} if the property has a setter method, otherwise {@code false}
     */
    default boolean isWritable() {
        return getSetter() != null;
    }

    /**
     * Determines whether this property is readable.
     *
     * @return {@code true} if the property has a getter method, otherwise {@code false}
     */
    default boolean isReadable() {
        return getGetter() != null;
    }

    /**
     * A default implementation of {@link PropertyDescriptor}.
     * <p>
     * This implementation provides a way to retrieve metadata about a property,
     * including its getter, setter, and owner.
     * </p>
     *
     * @param <T> the type of the object containing the property
     */
    abstract class Implementation<T> extends ElementDescriptor.Implementation<T> implements PropertyDescriptor<T> {

        private final Getter<T, Object>   getter;
        private final Setter<T, Object>   setter;
        private final ObjectDescriptor<T> owner;
        private final PropertyAccessor<T> accessor;

        /**
         * Constructs a new {@code PropertyDescriptor.Implementation}.
         *
         * @param name        the name of the property
         * @param internal    the internal representation of the property
         * @param annotations the annotations associated with this property
         * @param getter      the getter method for retrieving property values
         * @param setter      the setter method for modifying property values
         * @param owner       the owner descriptor that contains this property
         */
        Implementation(
                String name,
                T internal,
                Set<AnnotationDescriptor> annotations,
                Getter<T, Object> getter,
                Setter<T, Object> setter,
                ObjectDescriptor<T> owner
        ) {
            super(name, internal, annotations);

            this.getter = getter;
            this.setter = setter;
            this.owner = owner;
            this.accessor = PropertyAccessor.ofPropertyDescriptor(this);
        }

        /**
         * Returns the {@link PropertyAccessor} for this property.
         * <p>
         * The property accessor provides a mechanism to dynamically retrieve and modify property values.
         * </p>
         *
         * @return the property accessor for this descriptor
         */
        @Override
        public PropertyAccessor<T> getPropertyAccessor() {
            return accessor;
        }

        /**
         * Returns the owner of this property.
         * <p>
         * The owner is the object descriptor that contains this property.
         * </p>
         *
         * @return the {@link ObjectDescriptor} representing the owner of this property
         */
        @Override
        public ObjectDescriptor<T> getOwner() {
            return owner;
        }

        /**
         * Returns the getter method for this property.
         *
         * @param <V> the type of the value returned by the getter
         * @return the getter function for retrieving property values
         */
        @Override
        @SuppressWarnings({"unchecked"})
        public <V> Getter<T, V> getGetter() {
            return (Getter<T, V>) getter;
        }

        /**
         * Returns the setter method for this property.
         *
         * @param <V> the type of the value accepted by the setter
         * @return the setter function for modifying property values
         */
        @Override
        @SuppressWarnings({"unchecked"})
        public <V> Setter<T, V> getSetter() {
            return (Setter<T, V>) setter;
        }

        /**
         * Returns the class type of this property.
         *
         * @return the {@link Class} object representing the property type, or {@code null} if not available
         */
        @Override
        public Class<?> getClassType() {
            return getType() != null ? getType().getClassType() : null;
        }

        /**
         * Returns a string representation of the property descriptor.
         *
         * @return a formatted string containing the owner, property name, and property type
         */
        @Override
        public String toString() {
            return "%s#%s : %s".formatted(getOwner(), getName(), getType());
        }
    }

    /**
     * A builder class for constructing {@link PropertyDescriptor} instances.
     *
     * @param <B> the type of the builder itself
     * @param <T> the type of the object containing the property
     * @param <D> the type of {@link PropertyDescriptor} being built
     */
    abstract class Builder<B extends Builder<B, T, D>, T, D extends PropertyDescriptor<T>>
            extends ElementDescriptor.Builder<B, T, D> {

        protected Getter<T, Object>     getter;
        protected Setter<T, Object>     setter;
        protected JavaBeanDescriptor<T> owner;

        /**
         * Constructs a new {@code PropertyDescriptor.Builder}.
         *
         * @param name the name of the property being built
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the getter method for this property.
         *
         * @param getter the getter function
         * @return this builder instance
         */
        public B getter(Getter<T, Object> getter) {
            this.getter = getter;
            return self();
        }

        /**
         * Sets the setter method for this property.
         *
         * @param setter the setter function
         * @return this builder instance
         */
        public B setter(Setter<T, Object> setter) {
            this.setter = setter;
            return self();
        }

        /**
         * Sets the owner of this property.
         *
         * @param owner the {@link JavaBeanDescriptor} representing the owner
         * @return this builder instance
         */
        public B owner(JavaBeanDescriptor<T> owner) {
            this.owner = owner;
            return self();
        }
    }
}
