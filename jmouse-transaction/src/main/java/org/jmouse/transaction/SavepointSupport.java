package org.jmouse.transaction;

/**
 * Optional capability for savepoint-based transaction control.
 */
public interface SavepointSupport {

    Object createSavepoint();

    void rollbackToSavepoint(Object savepoint);

    void releaseSavepoint(Object savepoint);

}
