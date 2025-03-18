package org.jmouse.core.bind.introspection.structured.vo;

import org.jmouse.core.bind.introspection.AbstractIntrospector;
import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.MethodDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyData;
import org.jmouse.core.bind.introspection.structured.jb.JavaBeanPropertyIntrospector;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

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
     * Performs the introspection process to analyze the data container.
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
