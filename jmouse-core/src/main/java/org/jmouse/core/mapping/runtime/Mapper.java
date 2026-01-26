package org.jmouse.core.mapping.runtime;

public interface Mapper {
    <T> T map(Object source, Class<T> targetType);
}