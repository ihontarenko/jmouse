package org.jmouse.core.mapping.config;

/**
 * Controls how arrays are produced from iterable-like sources.
 */
public enum ArrayMaterializationPolicy {

    /**
     * Always materialize the source into an intermediate buffer (e.g. ArrayList)
     * before creating the final array.
     *
     * Pros: simplest, supports unknown-size iterables.
     * Cons: extra memory and pass.
     */
    MATERIALIZE,

    /**
     * Prefer direct streaming into the target array when source size is known.
     * If size is unknown, fallback to materialization.
     */
    STREAM_WHEN_SIZED,
}
