package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.runtime.MappingContext;

/**
 * Prebuilt mapping plan that can be executed multiple times.
 *
 * @param <T> target type
 */
public interface MappingPlan<T> {

    /**
     * Execute mapping and return populated target instance.
     */
    T execute(Object source, MappingContext context);
}