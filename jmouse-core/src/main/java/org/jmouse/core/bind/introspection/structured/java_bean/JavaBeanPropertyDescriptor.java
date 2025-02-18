package org.jmouse.core.bind.introspection.structured.java_bean;

import org.jmouse.core.bind.introspection.AbstractDescriptor;
import org.jmouse.core.bind.introspection.MethodDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyData;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

public class JavaBeanPropertyDescriptor<T>
        extends AbstractDescriptor<T, PropertyData<T>, JavaBeanPropertyIntrospector<T>>
        implements PropertyDescriptor<T> {

    protected JavaBeanPropertyDescriptor(JavaBeanPropertyIntrospector<T> introspector, PropertyData<T> container) {
        super(introspector, container);
    }

    @Override
    public Getter<T, Object> getGetter() {
        return container.getGetter();
    }

    @Override
    public void setGetter(Getter<T, Object> getter) {
        container.setGetter(getter);
    }

    @Override
    public Setter<T, Object> getSetter() {
        return container.getSetter();
    }

    @Override
    public void setSetter(Setter<T, Object> setter) {
        container.setSetter(setter);
    }

    @Override
    public MethodDescriptor getGetterMethod() {
        return container.getGetterMethod();
    }

    @Override
    public void setGetterMethod(MethodDescriptor getterMethod) {
        container.setGetterMethod(getterMethod);
    }

    @Override
    public MethodDescriptor getSetterMethod() {
        return container.getSetterMethod();
    }

    @Override
    public void setSetterMethod(MethodDescriptor setterMethod) {
        container.setSetterMethod(setterMethod);
    }

    @Override
    public JavaBeanPropertyIntrospector<T> toIntrospector() {
        return introspector;
    }

    @Override
    public ObjectDescriptor getOwner() {
        return container.getOwner();
    }

}
