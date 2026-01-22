package org.jmouse.core.mapping.virtuals;

/**
 * Virtual value container. Supports eager or lazy evaluation.
 */
public interface VirtualValue {

    Object get();

    static VirtualValue of(Object value) {
        return () -> value;
    }



}
