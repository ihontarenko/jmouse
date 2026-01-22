package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.access.SourcePropertyReader;
import org.jmouse.core.mapping.runtime.MappingContext;

@FunctionalInterface
public interface BeanPropertyOperation<T> {
    void apply(SourcePropertyReader reader, T target, MappingContext context);
}
