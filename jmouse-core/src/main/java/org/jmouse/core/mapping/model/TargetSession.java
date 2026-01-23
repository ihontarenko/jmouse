package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;

public interface TargetSession {

    Object instance();

    void write(PropertyDescriptor<Object> propertyDescriptor, Object value);

    void putConstructorArgument(String name, Object value);

    Object build();

}
