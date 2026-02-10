package org.jmouse.core.mapping.binding;

import org.jmouse.core.mapping.MappingContext;

@FunctionalInterface
public interface ValueTransformer {
    Object transform(Object value, MappingContext context);
}
