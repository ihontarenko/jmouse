package org.jmouse.core.bind.introspection.bean;

public class JavaBeanPropertyIntrospector<T> extends PropertyIntrospector<JavaBeanPropertyIntrospector<T>, T, JavaBeanPropertyDescriptor<T>> {

    protected JavaBeanPropertyIntrospector(T target) {
        super(target);
    }

    @Override
    public JavaBeanPropertyIntrospector<T> name() {
        return name("Prop");
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
