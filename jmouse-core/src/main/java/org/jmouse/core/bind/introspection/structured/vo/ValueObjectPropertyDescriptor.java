package org.jmouse.core.bind.introspection.structured.vo;

import org.jmouse.core.bind.introspection.AbstractDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyData;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

public class ValueObjectPropertyDescriptor<T>
        extends AbstractDescriptor<T, PropertyData<T>, ValueObjectPropertyIntrospector<T>>
        implements PropertyDescriptor<T> {

    public static final UnsupportedOperationException UNSUPPORTED_OPERATION_EXCEPTION = new UnsupportedOperationException(
            "Descriptor is immutable use introspector to modify it.");

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
        throw UNSUPPORTED_OPERATION_EXCEPTION;
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
        return null;
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
}
