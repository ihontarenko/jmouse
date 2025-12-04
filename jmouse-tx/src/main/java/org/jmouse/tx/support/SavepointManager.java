package org.jmouse.tx.support;

public interface SavepointManager {

    Object createSavepoint();

    void releaseSavepoint(Object savepoint);

    void rollbackSavepoint(Object savepoint);

}
