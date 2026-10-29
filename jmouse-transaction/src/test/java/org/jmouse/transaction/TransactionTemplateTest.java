package org.jmouse.transaction;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class TransactionTemplateTest {
    @Test
    void preservesRuntimeFailureAndAttachesRollbackFailure() {
        Manager manager = new Manager();
        RuntimeException original = new IllegalArgumentException("work failure");
        manager.rollbackFailure = new IllegalStateException("rollback failure");
        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> new TransactionTemplate(manager).inTransaction(null, () -> { throw original; }));
        assertSame(original, actual);
        assertArrayEquals(new Throwable[]{manager.rollbackFailure}, actual.getSuppressed());
        assertEquals(1, manager.rollbacks);
    }

    @Test
    void preservesJvmErrorIdentity() {
        Manager manager = new Manager();
        AssertionError original = new AssertionError("work failure");
        Error actual = assertThrows(Error.class,
                () -> new TransactionTemplate(manager).inTransaction(null, () -> { throw original; }));
        assertSame(original, actual);
        assertEquals(1, manager.rollbacks);
    }

    @Test
    void checkedFailureRemainsTheCause() {
        Manager manager = new Manager();
        IOException original = new IOException("work failure");
        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> new TransactionTemplate(manager).inTransaction(null, () -> { throw original; }));
        assertSame(original, actual.getCause());
    }

    @Test
    void doesNotRollbackAnAlreadyCompletedCommitFailure() {
        Manager manager = new Manager();
        manager.commitFailure = new IllegalStateException("completion failure");
        assertSame(manager.commitFailure, assertThrows(RuntimeException.class,
                () -> new TransactionTemplate(manager).inTransaction(null, () -> "value")));
        assertEquals(0, manager.rollbacks);
    }

    private static class Manager implements TransactionManager {
        private final Status status = new Status();
        RuntimeException rollbackFailure;
        RuntimeException commitFailure;
        int rollbacks;
        @Override public TransactionStatus begin(TransactionDefinition definition) { return status; }
        @Override public void commit(TransactionStatus value) { status.markCompleted(); if (commitFailure != null) throw commitFailure; }
        @Override public void rollback(TransactionStatus value) { rollbacks++; if (rollbackFailure != null) throw rollbackFailure; }
    }
    private static class Status implements TransactionStatus {
        private boolean completed;
        @Override public boolean isNew() { return true; }
        @Override public boolean isRollbackOnly() { return false; }
        @Override public void markRollbackOnly() { }
        @Override public boolean isCompleted() { return completed; }
        @Override public void markCompleted() { completed = true; }
    }
}
