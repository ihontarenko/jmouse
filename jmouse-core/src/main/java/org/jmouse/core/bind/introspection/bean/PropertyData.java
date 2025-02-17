package org.jmouse.core.bind.introspection.bean;

import org.jmouse.core.bind.introspection.internal.AbstractDataContainer;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

public class PropertyData<T> extends AbstractDataContainer<T> {

    private Getter<T, Object> getter;
    private Setter<T, Object> setter;

    public PropertyData(T target) {
        super(target);
    }

    public Getter<T, Object> getGetter() {
        return getter;
    }

    public void setGetter(Getter<T, Object> getter) {
        this.getter = getter;
    }

    public Setter<T, Object> getSetter() {
        return setter;
    }

    public void setSetter(Setter<T, Object> setter) {
        this.setter = setter;
    }
}
