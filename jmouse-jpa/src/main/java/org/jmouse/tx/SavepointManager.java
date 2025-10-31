package org.jmouse.tx;

public interface SavepointManager {

    Object createSavepoint();

    void releaseSavepoint(Object savepoint);

    void rollbackSavepoint(Object savepoint);

}
