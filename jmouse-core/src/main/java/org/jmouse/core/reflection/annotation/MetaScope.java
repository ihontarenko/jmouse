package org.jmouse.core.reflection.annotation;

public enum MetaScope {
    /**
     * Only the immediate meta of the visible annotation (parent).
     */
    DIRECT_ONLY,
    /**
     * All metas in breadth-first order (self excluded).
     */
    ALL
}