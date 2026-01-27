package org.jmouse.core.mapping;

@FunctionalInterface
public interface MapperProvider {
    Mapper get();
}