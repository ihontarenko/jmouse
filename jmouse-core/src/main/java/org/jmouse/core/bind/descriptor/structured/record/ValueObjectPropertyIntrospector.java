package org.jmouse.core.bind.descriptor.structured.record;

import org.jmouse.core.bind.descriptor.AbstractIntrospector;
import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;
import org.jmouse.core.bind.descriptor.MethodDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyData;
import org.jmouse.core.Getter;

public class ValueObjectPropertyIntrospector<T> extends
        AbstractIntrospector<PropertyData<T>, ValueObjectPropertyIntrospector<T>, T, ValueObjectPropertyDescriptor<T>> {

    public ValueObjectPropertyIntrospector(T target) {
        super(target);
    }

    /**
     * Automatically determines and sets the name for the introspected element.
     *
     * @return this introspector instance for method chaining
     */
    @Override
    public ValueObjectPropertyIntrospector<T> name() {
        return self();
    }

    public ValueObjectPropertyIntrospector<T> type(ClassTypeDescriptor type) {
        container.setType(type);
        return self();
    }

    public ValueObjectPropertyIntrospector<T> owner(ValueObjectDescriptor<T> owner) {
        container.setOwner(owner);
        return self();
    }

    public ValueObjectPropertyIntrospector<T> getter(Getter<T, Object> getter) {
        container.setGetter(getter);
        return self();
    }

    public ValueObjectPropertyIntrospector<T> getterMethod(MethodDescriptor descriptor) {
        container.setGetterMethod(descriptor);
        return getter(Getter.ofMethod(descriptor.unwrap()));
    }

    /**
     * Performs the descriptor process to analyze the data container.
     *
     * @return this introspector instance for method chaining
     */
    @Override
    public ValueObjectPropertyIntrospector<T> introspect() {
        return self();
    }

    /**
     * Converts the introspected data into a metadata descriptor.
     *
     * @return a {@link ValueObjectPropertyDescriptor} representing the analyzed data
     */
    @Override
    public ValueObjectPropertyDescriptor<T> toDescriptor() {
        return getCachedDescriptor(() -> new ValueObjectPropertyDescriptor<>(this, container));
    }

    @Override
    public PropertyData<T> getContainerFor(T target) {
        return new PropertyData<>(target);
    }
}
