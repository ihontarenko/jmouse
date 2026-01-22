package org.jmouse.core.mapping.access;

public interface SourcePropertyReader {

    boolean has(String name);

    Object read(String name);

    Class<?> sourceType();

}
