package org.jmouse.tx.core;

import java.util.Objects;

public final class DefaultTransactionDefinition implements TransactionDefinition {

    private final String                 name;
    private final TransactionPropagation propagation;
    private final TransactionIsolation   isolation;
    private final int                    timeoutSeconds;
    private final boolean                readOnly;

    private DefaultTransactionDefinition(Builder builder) {
        this.propagation = builder.propagation;
        this.isolation = builder.isolation;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.readOnly = builder.readOnly;
        this.name = builder.name;
    }

    @Override
    public TransactionPropagation getPropagation() {
        return propagation;
    }

    @Override
    public TransactionIsolation getIsolation() {
        return isolation;
    }

    @Override
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public String getName() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private TransactionPropagation propagation    = TransactionPropagation.REQUIRED;
        private TransactionIsolation   isolation      = TransactionIsolation.DEFAULT;
        private int                    timeoutSeconds = -1;
        private boolean                readOnly;
        private String                 name;

        public Builder propagation(TransactionPropagation propagation) {
            this.propagation = Objects.requireNonNull(propagation);
            return this;
        }

        public Builder isolation(TransactionIsolation isolation) {
            this.isolation = Objects.requireNonNull(isolation);
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public DefaultTransactionDefinition build() {
            return new DefaultTransactionDefinition(this);
        }

    }

}
