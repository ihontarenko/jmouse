package org.jmouse.mapper;

@FunctionalInterface
public interface MapperProvider {

    /**
     * Return a mapper instance bound to the given context.
     */
    Mapper get(MappingContext context);
}