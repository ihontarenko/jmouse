package org.jmouse.core.mapping;

import org.jmouse.core.reflection.InferredType;

public interface Mapper {

    default <T> T map(Object source, Class<T> targetType) {
        return map(source, InferredType.forType(targetType));
    }

    <T> T map(Object source, InferredType type);

}