package org.jmouse.core.mapping.targets;

import org.jmouse.core.bind.PropertyPath;

/**
 * Target write facade for beans, maps, and future immutable construction.
 */
public interface TargetWriter {

    /**
     * Write a value into the target using a property path.
     * For example: {@code "books[0].title"}.
     */
    void write(PropertyPath path, Object value);

    /**
     * @return underlying target instance.
     */
    Object target();
}
