package org.jmouse.core.bind.introspection.bean;

public class JavaBeanPropertyDescriptor<T> extends PropertyDescriptor<T, JavaBeanPropertyIntrospector<T>> {

    protected JavaBeanPropertyDescriptor(JavaBeanPropertyIntrospector<T> introspector, PropertyData<T> container) {
        super(introspector, container);
    }

}
