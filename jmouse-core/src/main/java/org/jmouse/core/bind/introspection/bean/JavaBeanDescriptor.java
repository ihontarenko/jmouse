package org.jmouse.core.bind.introspection.bean;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.reflection.ClassTypeInspector;

public class JavaBeanDescriptor<T> extends ObjectDescriptor<Class<T>, JavaBeanIntrospector<T>>
        implements ClassTypeInspector {

    protected JavaBeanDescriptor(JavaBeanIntrospector<T> introspector, ObjectData<Class<T>> container) {
        super(introspector, container);
    }

    public ClassTypeDescriptor getType() {
        return container.getType();
    }

    @Override
    public JavaBeanIntrospector<T> toIntrospector() {
        return introspector;
    }

    @Override
    public String toString() {
        return "Java-Bean Descriptor: " + container.getType();
    }

    @Override
    public Class<?> getClassType() {
        return unwrap();
    }

}
