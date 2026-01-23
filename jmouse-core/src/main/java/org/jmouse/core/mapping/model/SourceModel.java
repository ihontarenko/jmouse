package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.mapping.values.ValueKind;

public interface SourceModel {

    Object source();

    Class<?> sourceType();

    ValueKind kind();

    ObjectDescriptor<?> descriptor();

    boolean has(String name);

    Object read(String name);

}
