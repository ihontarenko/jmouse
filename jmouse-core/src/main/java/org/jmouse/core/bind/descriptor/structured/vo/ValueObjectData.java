package org.jmouse.core.bind.descriptor.structured.vo;

import org.jmouse.core.bind.descriptor.ConstructorDescriptor;
import org.jmouse.core.bind.descriptor.structured.ObjectData;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;

public class ValueObjectData<T> extends ObjectData<T> {

    private       ConstructorDescriptor              constructor;
    private final Map<String, PropertyDescriptor<T>> components = new LinkedHashMap<>();

    public ValueObjectData(T target) {
        super(target);
    }

    public ConstructorDescriptor getConstructor() {
        return constructor;
    }

    public void setConstructor(ConstructorDescriptor constructor) {
        this.constructor = constructor;
    }

    public void addComponent(PropertyDescriptor<T> descriptor) {
        getComponents().put(descriptor.getName(), descriptor);
    }

    public Map<String, PropertyDescriptor<T>> getComponents() {
        return components;
    }

}
