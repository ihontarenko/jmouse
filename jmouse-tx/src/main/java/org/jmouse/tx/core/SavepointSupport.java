package org.jmouse.tx.core;

/**
 * Optional capability for savepoint-based transaction control.
 */
public interface SavepointSupport {

    Object createSavepoint();

    void rollbackToSavepoint(Object savepoint);

    void releaseSavepoint(Object savepoint);

}
