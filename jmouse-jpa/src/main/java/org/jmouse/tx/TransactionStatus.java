package org.jmouse.tx;

/**
 * ⚙️ Live transaction state for current thread.
 */
public interface TransactionStatus {

    boolean isNew();

    boolean isCompleted();

    boolean isRollbackOnly();

    void setRollbackOnly();

    Object getSuspended();

    Object getSavepoint();

    void setSavepoint(Object savepoint);

    Object getResource();

    /**
     * 📦 basic impl.
     */
    final class Simple implements TransactionStatus {
        private final boolean isNew;
        private boolean completed;
        private boolean rollbackOnly;
        private final Object resource;
        private final Object suspended;
        private Object savepoint;

        public Simple(boolean isNew, Object resource, Object suspended) {
            this.isNew = isNew;
            this.resource = resource;
            this.suspended = suspended;
        }

        @Override public boolean isNew() { return isNew; }
        @Override public boolean isCompleted() { return completed; }
        @Override public boolean isRollbackOnly() { return rollbackOnly; }
        @Override public void setRollbackOnly() { this.rollbackOnly = true; }
        @Override public Object getSuspended() { return suspended; }
        @Override public Object getSavepoint() { return savepoint; }
        @Override public void setSavepoint(Object savepoint) { this.savepoint = savepoint; }
        @Override public Object getResource() { return resource; }

        public void markCompleted() { this.completed = true; }
    }
}
