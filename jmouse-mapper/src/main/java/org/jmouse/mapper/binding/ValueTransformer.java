package org.jmouse.mapper.binding;

import org.jmouse.mapper.MappingContext;

@FunctionalInterface
public interface ValueTransformer {
    Object transform(Object value, MappingContext context);
}
