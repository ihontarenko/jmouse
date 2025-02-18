package org.jmouse.core.bind.introspection.structured.java_bean;

import org.jmouse.core.bind.introspection.AbstractIntrospector;
import org.jmouse.core.bind.introspection.MethodDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyData;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

public class JavaBeanPropertyIntrospector<T>
        extends AbstractIntrospector<PropertyData<T>, JavaBeanPropertyIntrospector<T>, T, JavaBeanPropertyDescriptor<T>> {

    protected JavaBeanPropertyIntrospector(T target) {
        super(target);
    }

    JavaBeanPropertyIntrospector<T> owner(JavaBeanDescriptor<T> descriptor) {
        container.setOwner(descriptor);
        return self();
    }

    public JavaBeanPropertyIntrospector<T> getter(Getter<T, Object> getter) {
        container.setGetter(getter);
        return self();
    }

    public JavaBeanPropertyIntrospector<T> setter(Setter<T, Object> setter) {
        container.setSetter(setter);
        return self();
    }

    public JavaBeanPropertyIntrospector<T> getterMethod(MethodDescriptor descriptor) {
        container.setGetterMethod(descriptor);
        return getter(Getter.ofMethod(descriptor.unwrap()));
    }

    public JavaBeanPropertyIntrospector<T> setterMethod(MethodDescriptor descriptor) {
        container.setSetterMethod(descriptor);
        return setter(Setter.ofMethod(descriptor.unwrap()));
    }

    @Override
    public JavaBeanPropertyIntrospector<T> name() {
        return self();
    }

    @Override
    public JavaBeanPropertyIntrospector<T> introspect() {
        return self();
    }

    @Override
    public JavaBeanPropertyDescriptor<T> toDescriptor() {
        return getCachedDescriptor(() -> new JavaBeanPropertyDescriptor<>(this, container));
    }

    @Override
    public PropertyData<T> getContainerFor(T target) {
        return new PropertyData<>(target);
    }

}
