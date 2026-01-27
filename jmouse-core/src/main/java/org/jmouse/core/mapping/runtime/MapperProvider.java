package org.jmouse.core.mapping.runtime;

@FunctionalInterface
public interface MapperProvider {
    Mapper get();
}