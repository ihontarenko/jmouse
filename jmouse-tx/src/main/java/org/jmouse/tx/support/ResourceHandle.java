package org.jmouse.tx.support;

public interface ResourceHandle {
    void begin();
    void commit();
    void rollback();
    void suspend();
    void resume();
}
