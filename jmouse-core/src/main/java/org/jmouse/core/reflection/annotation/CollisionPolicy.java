package org.jmouse.core.reflection.annotation;

public enum CollisionPolicy {
    /**
     * Keep the already-present key (visible annotation has priority).
     */
    KEEP_EXISTING,
    /**
     * Replace existing value when meta provides the same key.
     */
    OVERWRITE,
}