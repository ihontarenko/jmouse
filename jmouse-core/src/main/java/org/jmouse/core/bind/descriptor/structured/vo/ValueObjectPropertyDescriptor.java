package org.jmouse.core.bind.descriptor.structured.vo;

import org.jmouse.core.bind.descriptor.AbstractDescriptor;
import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;
import org.jmouse.core.bind.descriptor.MethodDescriptor;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyData;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

public class ValueObjectPropertyDescriptor<T>
        extends AbstractDescriptor<T, PropertyData<T>, ValueObjectPropertyIntrospector<T>>
        implements PropertyDescriptor<T> {

    public static final UnsupportedOperationException UNSUPPORTED_OPERATION_EXCEPTION = new UnsupportedOperationException(
            "Descriptor is immutable unable to modify it.");

    protected ValueObjectPropertyDescriptor(ValueObjectPropertyIntrospector<T> introspector, PropertyData<T> container) {
        super(introspector, container);
    }

    @Override
    public ValueObjectPropertyIntrospector<T> toIntrospector() {
        return introspector;
    }

    /**
     * Returns the type descriptor of this property.
     * <p>
     * The returned {@link ClassTypeDescriptor} provides detailed metadata about the property's type.
     * </p>
     *
     * @return the type descriptor of this property
     */
    @Override
    public ClassTypeDescriptor getType() {
        return container.getType();
    }

    /**
     * Sets the type descriptor for this property.
     * <p>
     * This method allows updating the type descriptor, which may be useful in cases where
     * the type needs to be inferred dynamically or adjusted post-initialization.
     * </p>
     *
     * @param type the new type descriptor for this property
     */
    @Override
    public void setType(ClassTypeDescriptor type) {
        container.setType(type);
    }

    /**
     * Returns the owner descriptor of this property.
     *
     * @return the {@link ObjectDescriptor} that owns this property
     */
    @Override
    public ObjectDescriptor<T> getOwner() {
        return container.getOwner();
    }

    /**
     * Returns the getter for this property.
     *
     * @return the getter function, or {@code null} if not available
     */
    @Override
    public Getter<T, Object> getGetter() {
        return container.getGetter();
    }

    /**
     * Returns the getter method descriptor for this property.
     *
     * @return the getter method descriptor, or {@code null} if not available
     */
    @Override
    public MethodDescriptor getGetterMethod() {
        return container.getGetterMethod();
    }

    /**
     * Sets the getter for this property.
     *
     * @param getter the getter function
     */
    @Override
    public void setGetter(Getter<T, ?> getter) {
        container.setGetter(getter);
    }

    /**
     * Returns the setter for this property.
     *
     * @return the setter function, or {@code null} if not available
     */
    @Override
    public Setter<T, Object> getSetter() {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    /**
     * Sets the setter for this property.
     *
     * @param setter the setter function
     */
    @Override
    public void setSetter(Setter<T, ?> setter) {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    /**
     * Returns a string representation of this JavaBean property descriptor.
     *
     * @return a formatted string representing the property name and type
     */
    @Override
    public String toString() {
        return "[%s]: %s".formatted(getName(), getType().getJavaType());
    }
}
