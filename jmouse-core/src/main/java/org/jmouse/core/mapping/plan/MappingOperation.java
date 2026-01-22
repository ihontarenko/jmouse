package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.runtime.MappingContext;

/**
 * A single mapping step that mutates the target instance.
 *
 * @param <T> target type
 */
public interface MappingOperation<T> {
    void apply(Object source, T target, MappingContext context);
}