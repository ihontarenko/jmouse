package org.jmouse.mapper.binding;

import org.jmouse.mapper.MappingContext;

@FunctionalInterface
public interface ComputeFunction<S> {
    Object compute(S source, MappingContext context);
}
