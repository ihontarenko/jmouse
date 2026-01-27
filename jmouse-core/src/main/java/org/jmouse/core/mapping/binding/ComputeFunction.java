package org.jmouse.core.mapping.binding;

import org.jmouse.core.mapping.MappingContext;

@FunctionalInterface
public interface ComputeFunction<S> {
    Object compute(S source, MappingContext context);
}
