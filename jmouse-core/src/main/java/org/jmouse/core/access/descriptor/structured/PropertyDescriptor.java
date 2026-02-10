package org.jmouse.core.access.descriptor.structured;

import org.jmouse.core.access.DirectPropertyAccess;
import org.jmouse.core.access.LazyPropertyAccess;
import org.jmouse.core.access.PropertyAccessor;
import org.jmouse.core.access.descriptor.ClassTypeDescriptor;
import org.jmouse.core.access.descriptor.MethodDescriptor;
import org.jmouse.core.Getter;
import org.jmouse.core.Setter;

import java.util.function.Supplier;

/**
 * Represents a property descriptor that provides metadata and access mechanisms
 * for a property within an {@link ObjectDescriptor}.
 * <p>
 * This interface defines methods for retrieving property metadata, including its
 * getter and setter methods, as well as checking its readability and writability.
 * </p>
 *
 * @param <T> the type of the object that owns this property
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public interface PropertyDescriptor<T> {

    /**
     * Returns the type descriptor of this property.
     * <p>
     * The returned {@link ClassTypeDescriptor} provides detailed metadata about the property's type.
     * </p>
     *
     * @return the type descriptor of this property
     */
    ClassTypeDescriptor getType();

    /**
     * Sets the type descriptor for this property.
     * <p>
     * This method allows updating the type descriptor, which may be useful in cases where
     * the type needs to be inferred dynamically or adjusted post-initialization.
     * </p>
     *
     * @param type the new type descriptor for this property
     */
    void setType(ClassTypeDescriptor type);

    /**
     * Returns the name of this property.
     *
     * @return the property name
     */
    String getName();

    /**
     * Returns the owner descriptor of this property.
     *
     * @return the {@link ObjectDescriptor} that owns this property
     */
    ObjectDescriptor<T> getOwner();

    /**
     * Returns the getter for this property.
     *
     * @return the getter function, or {@code null} if not available
     */
    Getter<T, Object> getGetter();

    /**
     * Sets the getter for this property.
     *
     * @param getter the getter function
     */
    void setGetter(Getter<T, ?> getter);

    /**
     * Returns the setter for this property.
     *
     * @return the setter function, or {@code null} if not available
     */
    Setter<T, Object> getSetter();

    /**
     * Sets the setter for this property.
     *
     * @param setter the setter function
     */
    void setSetter(Setter<T, ?> setter);

    /**
     * Checks if the property is readable (i.e., has a getter).
     *
     * @return {@code true} if the property has a getter, {@code false} otherwise
     */
    default boolean isReadable() {
        return getGetter() != null;
    }

    /**
     * Checks if the property is writable (i.e., has a setter).
     *
     * @return {@code true} if the property has a setter, {@code false} otherwise
     */
    default boolean isWritable() {
        return getSetter() != null /*&& (getSetterMethod().unwrap().getModifiers() & Modifier.ABSTRACT) == 0*/;
    }

    /**
     * Retrieves the {@link PropertyAccessor} associated with this object.
     *
     * @return a {@link PropertyAccessor} instance, or {@code null} if not provided
     */
    default PropertyAccessor<T> getAccessor() {
        return DirectPropertyAccess.forPropertyDescriptor(this);
    }

    /**
     * Retrieves the {@link PropertyAccessor} associated with this object.
     *
     * @return a {@link PropertyAccessor} instance, or {@code null} if not provided
     */
    default PropertyAccessor<Supplier<T>> getLazyAccessor() {
        return LazyPropertyAccess.forPropertyDescriptor(this);
    }

    /**
     * Returns the method descriptor for the getter of this property.
     * <p>
     * By default, this method throws an {@link UnsupportedOperationException}
     * unless explicitly overridden in an implementation.
     * </p>
     *
     * @return the method descriptor of the getter
     * @throws UnsupportedOperationException if this operation is not supported
     */
    default MethodDescriptor getGetterMethod() {
        throw new UnsupportedOperationException(
                "Unable to get getter method for '%s' descriptor".formatted(getName()));
    }

    /**
     * Sets the method descriptor for the getter of this property.
     * <p>
     * By default, this method throws an {@link UnsupportedOperationException}
     * unless explicitly overridden in an implementation.
     * </p>
     *
     * @param getterMethod the method descriptor of the getter
     * @throws UnsupportedOperationException if this operation is not supported
     */
    default void setGetterMethod(MethodDescriptor getterMethod) {
        throw new UnsupportedOperationException(
                "Unable to set getter method for '%s' descriptor".formatted(getName()));
    }

    /**
     * Returns the method descriptor for the setter of this property.
     * <p>
     * By default, this method throws an {@link UnsupportedOperationException}
     * unless explicitly overridden in an implementation.
     * </p>
     *
     * @return the method descriptor of the setter
     * @throws UnsupportedOperationException if this operation is not supported
     */
    default MethodDescriptor getSetterMethod() {
        throw new UnsupportedOperationException(
                "Unable to get setter method for '%s' descriptor".formatted(getName()));
    }

    /**
     * Sets the method descriptor for the setter of this property.
     * <p>
     * By default, this method throws an {@link UnsupportedOperationException}
     * unless explicitly overridden in an implementation.
     * </p>
     *
     * @param setterMethod the method descriptor of the setter
     * @throws UnsupportedOperationException if this operation is not supported
     */
    default void setSetterMethod(MethodDescriptor setterMethod) {
        throw new UnsupportedOperationException(
                "Unable to set setter method for '%s' descriptor".formatted(getName()));
    }

}
