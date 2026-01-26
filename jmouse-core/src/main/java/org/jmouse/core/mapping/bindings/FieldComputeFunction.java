package org.jmouse.core.mapping.bindings;

import org.jmouse.core.mapping.runtime.MappingContext;

@FunctionalInterface
public interface FieldComputeFunction {
    Object compute(Object source, MappingContext context);
}
