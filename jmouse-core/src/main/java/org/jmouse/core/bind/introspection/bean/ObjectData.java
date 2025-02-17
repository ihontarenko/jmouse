package org.jmouse.core.bind.introspection.bean;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.internal.AbstractDataContainer;

import java.util.Map;

public class ObjectData<T> extends AbstractDataContainer<T> {

    private ClassTypeDescriptor type;
    private Map<String, PropertyDescriptor<? extends T, ?>> properties;

    public ObjectData(T target) {
        super(target);
    }

    public ClassTypeDescriptor getType() {
        return type;
    }

    public void setType(ClassTypeDescriptor type) {
        this.type = type;
    }

    public Map<String, PropertyDescriptor<? extends T, ?>> getProperties() {
        return properties;
    }

    public <P extends PropertyDescriptor<T, ?>> P getProperty(String name) {
        return (P) properties.get(name);
    }

    public void addProperty(PropertyDescriptor<? extends T, ?> property) {
        properties.put(property.getName(), property);
    }

}
