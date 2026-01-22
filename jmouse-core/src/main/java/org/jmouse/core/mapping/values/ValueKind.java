package org.jmouse.core.mapping.values;

/**
 * High-level classification of a value for mapping purposes.
 */
public enum ValueKind {
    NULL,
    SCALAR,
    JAVA_BEAN,
    VALUE_OBJECT,
    COLLECTION,
    MAP
}
