package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.mapping.values.ValueKind;

public interface TargetModel {

    Class<?> targetType();

    ValueKind kind();

    ObjectDescriptor<?> descriptor();

    TargetSession newSession();

}
