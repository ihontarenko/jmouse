package org.jmouse.core.bind.introspection.structured;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.internal.AbstractDataContainer;

import java.util.HashMap;
import java.util.Map;

public class ObjectData<T> extends AbstractDataContainer<T> implements ObjectDescriptor<T> {

    private final Map<String, PropertyDescriptor<T>> properties = new HashMap<>();
    private       ClassTypeDescriptor                type;

    public ObjectData(T target) {
        super(target);
    }

    @Override
    public ClassTypeDescriptor getType() {
        return type;
    }

    @Override
    public void setType(ClassTypeDescriptor type) {
        this.type = type;
    }

    @Override
    public Map<String, PropertyDescriptor<T>> getProperties() {
        return properties;
    }

}
