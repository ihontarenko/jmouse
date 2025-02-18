package org.jmouse.core.bind.introspection.structured;

import org.jmouse.core.bind.introspection.MethodDescriptor;
import org.jmouse.core.bind.introspection.internal.AbstractDataContainer;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

public class PropertyData<T> extends AbstractDataContainer<T> implements PropertyDescriptor<T> {

    private Getter<T, Object>   getter;
    private Setter<T, Object>   setter;
    private MethodDescriptor    getterMethod;
    private MethodDescriptor    setterMethod;
    private ObjectDescriptor<T> owner;

    public PropertyData(T target) {
        super(target);
    }

    @Override
    public Getter<T, Object> getGetter() {
        return getter;
    }

    @Override
    public void setGetter(Getter<T, Object> getter) {
        this.getter = getter;
    }

    @Override
    public Setter<T, Object> getSetter() {
        return setter;
    }

    @Override
    public void setSetter(Setter<T, Object> setter) {
        this.setter = setter;
    }

    @Override
    public MethodDescriptor getGetterMethod() {
        return getterMethod;
    }

    @Override
    public void setGetterMethod(MethodDescriptor getterMethod) {
        this.getterMethod = getterMethod;
    }

    @Override
    public MethodDescriptor getSetterMethod() {
        return setterMethod;
    }

    @Override
    public void setSetterMethod(MethodDescriptor setterMethod) {
        this.setterMethod = setterMethod;
    }

    @Override
    public ObjectDescriptor<T> getOwner() {
        return owner;
    }

    public void setOwner(ObjectDescriptor<T> owner) {
        this.owner = owner;
    }
}
