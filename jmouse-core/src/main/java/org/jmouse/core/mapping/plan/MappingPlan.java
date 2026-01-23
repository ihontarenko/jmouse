package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.runtime.MappingContext;

public interface MappingPlan<T> {
    T execute(Object source, MappingContext context);
}