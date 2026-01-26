package org.jmouse.core.mapping.bindings;

import org.jmouse.core.mapping.runtime.MappingContext;

@FunctionalInterface
public interface ComputeFunction<S> {
    Object compute(S source, MappingContext context);
}
