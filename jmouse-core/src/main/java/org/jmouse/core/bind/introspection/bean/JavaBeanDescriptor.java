package org.jmouse.core.bind.introspection.bean;

public class JavaBeanDescriptor<T> extends ObjectDescriptor<Class<T>, JavaBeanDescriptor<T>, JavaBeanIntrospector<T>> {

    protected JavaBeanDescriptor(JavaBeanIntrospector<T> introspector, ObjectData<Class<T>> container) {
        super(introspector, container);
    }

    @Override
    public JavaBeanIntrospector<T> toIntrospector() {
        return introspector;
    }

}
