package org.jmouse.core.reflection.annotation;

/**
 * ⚖️ Merge policy when the same key appears multiple times.
 */
public enum MergePolicy {
    /**
     * First non-default wins (breadth-first from root)
     */
    FIRST_WINS,
    /**
     * Last non-default wins (deepest meta overrides)
     */
    LAST_WINS,
    /**
     * Collect all distinct values in insertion order (as List)
     */
    COLLECT
}