package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.MappingContext;

public interface MappingPlan<T> {
    T execute(Object source, MappingContext context);
}
