package org.jmouse.core.bind.introspection.structured.java_bean;

import org.jmouse.core.bind.introspection.AbstractDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectData;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;
import org.jmouse.core.reflection.ClassTypeInspector;

import java.util.Map;

public class JavaBeanDescriptor<T>
        extends AbstractDescriptor<T, ObjectData<T>, JavaBeanIntrospector<T>> implements ClassTypeInspector, ObjectDescriptor<T> {

    protected JavaBeanDescriptor(JavaBeanIntrospector<T> introspector, ObjectData<T> container) {
        super(introspector, container);
    }

    public ClassTypeDescriptor getType() {
        return container.getType();
    }

    @Override
    public void setType(ClassTypeDescriptor type) {
        container.setType(type);
    }

    @Override
    public Map<String, PropertyDescriptor<T>> getProperties() {
        return container.getProperties();
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
        return container.getType().getClassType();
    }

}
