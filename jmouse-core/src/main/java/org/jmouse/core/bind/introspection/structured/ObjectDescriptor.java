package org.jmouse.core.bind.introspection.structured;

import org.jmouse.core.bind.PropertyAccessor;
import org.jmouse.core.bind.introspection.ClassTypeDescriptor;

import java.util.Map;

public interface ObjectDescriptor<T> {

    default PropertyAccessor<T> getPropertyAccessor() {
        return null;
    }

    ClassTypeDescriptor getType();

    void setType(ClassTypeDescriptor type);

    Map<String, PropertyDescriptor<T>> getProperties();

    default boolean hasProperty(String name) {
        return getProperties().containsKey(name);
    }

    default PropertyDescriptor<T> getProperty(String name) {
        return getProperties().get(name);
    }

    default void addProperty(PropertyDescriptor<T> property) {
        getProperties().put(property.getName(), property);
    }

}
