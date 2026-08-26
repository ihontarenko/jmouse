package org.jmouse.core.access.descriptor.structured.record;

import org.jmouse.core.access.descriptor.AbstractDescriptor;
import org.jmouse.core.access.descriptor.ClassTypeDescriptor;
import org.jmouse.core.access.descriptor.MethodDescriptor;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyData;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.Getter;
import org.jmouse.core.Setter;

public class ValueObjectPropertyDescriptor<T>
        extends AbstractDescriptor<T, PropertyData<T>, ValueObjectPropertyIntrospector<T>>
        implements PropertyDescriptor<T> {

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
        throw noSetter();
    }

    /**
     * Sets the setter for this property.
     *
     * @param setter the setter function
     */
    @Override
    public void setSetter(Setter<T, ?> setter) {
        throw noSetter();
    }

    /**
     * {@inheritDoc}
     *
     * <h2>⚠️ Answered, not thrown — a predicate with a third outcome is not a predicate</h2>
     *
     * <p>The inherited default is {@code getSetter() != null}, and {@link #getSetter()} here refuses.
     * So asking a record's component the ordinary question <em>"can this be written?"</em> used to blow
     * up, which meant every caller had to know what kind of descriptor it was holding before it dared
     * ask — defeating the point of asking through the interface at all. It reached a screen: the mapping
     * builder listed a product's response types and could not open a single one of them, because they
     * were records.</p>
     *
     * <p>⚠️ {@code false} is the honest answer to <em>this</em> question, and it is not the whole truth
     * about a record. A component <strong>can</strong> be filled — through the constructor — which is
     * what makes a record a legal mapping target. That is a different question, and it is asked of
     * {@link org.jmouse.core.access.descriptor.structured.record.ValueObjectDescriptor#getComponents()}.
     * Anything deciding whether it may write into a type has to ask both.</p>
     */
    @Override
    public boolean isWritable() {
        return false;
    }

    /**
     * Says there is no setter, at the place where somebody asked for one.
     *
     * <h2>⚠️ Constructed here rather than shared, because a stack trace is captured where the exception
     * is BUILT</h2>
     *
     * <p>This used to be one {@code static final} instance created in the class initializer. Every throw
     * for the rest of the JVM's life then carried the frames of whoever happened to load the class first
     * — a trace that pointed confidently at an introspector doing nothing wrong, several frames away from
     * the call that actually failed. It cost real time to read past. An exception on a should-never-happen
     * path is not worth pre-allocating, and a trace naming the wrong place is worse than none.</p>
     *
     * @return the refusal, naming what was asked for
     */
    private UnsupportedOperationException noSetter() {
        return new UnsupportedOperationException(
                ("'%s' is a component of a record and has no setter — a record is built through its "
                 + "constructor. Ask isWritable() first, or take the components from "
                 + "ValueObjectDescriptor.getComponents().").formatted(getName()));
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
