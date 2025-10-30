package org.jmouse.tx;

public interface TransactionStatus {

    boolean isNew();

    boolean isCompleted();

    boolean isRollbackOnly();

    void setRollbackOnly();

    Object getSuspendedResources();

    Object getSavepoint();

    void setSavepoint(Object savepoint);

    Object getResource();

    void markCompleted();

    default boolean hasSavepoint() {
        return getSavepoint() != null;
    }

    final class Simple implements TransactionStatus {

        private final boolean isNew;
        private final Object  resource;
        private final Object  suspended;
        private       boolean completed;
        private       boolean rollbackOnly;
        private       Object  savepoint;

        public Simple(String name, boolean isNew, Object resource, Object suspended) {
            this.isNew = isNew;
            this.resource = resource;
            this.suspended = suspended;
        }

        public Simple(boolean isNew, Object resource, Object suspended) {
            this(null, isNew, resource, suspended);
        }

        @Override
        public boolean isNew() {
            return isNew;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public void setRollbackOnly() {
            this.rollbackOnly = true;
        }

        @Override
        public Object getSuspendedResources() {
            return suspended;
        }

        @Override
        public Object getSavepoint() {
            return savepoint;
        }

        @Override
        public void setSavepoint(Object savepoint) {
            this.savepoint = savepoint;
        }

        @Override
        public Object getResource() {
            return resource;
        }

        @Override
        public void markCompleted() {
            this.completed = true;
        }

    }
}
