package org.jmouse.tx;

/**
 * Transaction isolation levels. 🔒
 */
public enum IsolationLevel {
    DEFAULT,
    READ_UNCOMMITTED,
    READ_COMMITTED,
    REPEATABLE_READ,
    SERIALIZABLE
}