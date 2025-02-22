package org.jmouse.core.bind.introspection.structured.vo;

import org.jmouse.core.bind.introspection.ConstructorDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectData;

public class ValueObjectData<T> extends ObjectData<T> {

    private ConstructorDescriptor constructor;

    public ValueObjectData(T target) {
        super(target);
    }

    public ConstructorDescriptor getConstructor() {
        return constructor;
    }

    public void setConstructor(ConstructorDescriptor constructor) {
        this.constructor = constructor;
    }
}
