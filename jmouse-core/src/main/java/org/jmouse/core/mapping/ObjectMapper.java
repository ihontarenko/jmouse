package org.jmouse.core.mapping;

public interface ObjectMapper {

    <T> T map(Object source, Class<T> targetType);

    void map(Object source, Object target);
}
