package org.jmouse.tx;

/**
 * 🎯 Describes transaction requirements.
 */
public interface TransactionDefinition {

    int PROPAGATION_REQUIRED      = 0;
    int PROPAGATION_REQUIRES_NEW  = 1;
    int PROPAGATION_SUPPORTS      = 2;
    int PROPAGATION_MANDATORY     = 3;
    int PROPAGATION_NEVER         = 4;
    int PROPAGATION_NOT_SUPPORTED = 5;
    int PROPAGATION_NESTED        = 6;

    int ISOLATION_DEFAULT          = -1;
    int ISOLATION_READ_UNCOMMITTED = 1;
    int ISOLATION_READ_COMMITTED   = 2;
    int ISOLATION_REPEATABLE_READ  = 4;
    int ISOLATION_SERIALIZABLE     = 8;

    int TIMEOUT_DEFAULT = -1;

    static TransactionDefinition withDefaults() {
        return new Simple(
                PROPAGATION_REQUIRED,
                ISOLATION_DEFAULT,
                TIMEOUT_DEFAULT,
                false,
                null
        );
    }

    int getPropagation();

    int getIsolation();

    int getTimeout();

    boolean isReadOnly();

    String getName();

    final class Simple implements TransactionDefinition {

        private final String  name;
        private final int     propagation;
        private final int     isolation;
        private final int     timeout;
        private final boolean readOnly;

        public Simple(int propagation, int isolation, int timeout, boolean readOnly, String name) {
            this.propagation = propagation;
            this.isolation = isolation;
            this.timeout = timeout;
            this.readOnly = readOnly;
            this.name = name;
        }

        public static Simple required() {
            return new Simple(PROPAGATION_REQUIRED, ISOLATION_DEFAULT, 0, false, null);
        }

        @Override
        public int getPropagation() {
            return propagation;
        }

        @Override
        public int getIsolation() {
            return isolation;
        }

        @Override
        public int getTimeout() {
            return timeout;
        }

        @Override
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public String getName() {
            return name;
        }

    }

}
