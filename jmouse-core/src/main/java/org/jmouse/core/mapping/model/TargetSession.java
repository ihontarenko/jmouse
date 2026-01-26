package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;

public interface TargetSession {

    boolean isRecord();

    Object instance(); // beans only; for records returns null

    void write(PropertyDescriptor<?> property, Object value);

    void putConstructorArgument(String name, Object value);

    Object build();

}
