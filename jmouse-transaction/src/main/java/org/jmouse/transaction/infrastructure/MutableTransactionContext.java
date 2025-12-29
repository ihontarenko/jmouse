package org.jmouse.transaction.infrastructure;

import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.TransactionStatus;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class MutableTransactionContext implements TransactionContext {

    private final TransactionDefinition definition;
    private final TransactionStatus     status;
    private final TransactionSession    session;
    private       Object                savepoint;
    private       Integer               effectiveTimeoutSeconds;
    private       TransactionAttributes attributes;

    public MutableTransactionContext(TransactionDefinition definition, TransactionStatus status, TransactionSession session) {
        this.definition = definition;
        this.status = status;
        this.session = session;
    }

    @Override
    public TransactionDefinition getDefinition() {
        return definition;
    }

    @Override
    public TransactionStatus getStatus() {
        return status;
    }

    @Override
    public TransactionSession getSession() {
        return session;
    }

    @Override
    public Optional<Object> getSavepoint() {
        return Optional.ofNullable(savepoint);
    }

    @Override
    public boolean isRollbackOnly() {
        return status.isRollbackOnly();
    }

    @Override
    public void setRollbackOnly() {
        status.markRollbackOnly();
    }

    public void setSavepoint(Object savepoint) {
        this.savepoint = savepoint;
    }

    public void clearSavepoint() {
        this.savepoint = null;
    }

    public Optional<Integer> getEffectiveTimeoutSeconds() {
        return Optional.ofNullable(effectiveTimeoutSeconds);
    }

    public void setEffectiveTimeoutSeconds(Integer seconds) {
        this.effectiveTimeoutSeconds = seconds;
    }

    public Optional<TransactionAttributes> getAttributes() {
        return Optional.ofNullable(attributes);
    }

    public void setAttributes(TransactionAttributes attributes) {
        this.attributes = attributes;
    }

    public int remainingTimeoutSeconds() {
        if (attributes == null || !attributes.hasTimeout()) {
            return 0;
        }

        long leftNanos = attributes.deadlineNanos() - System.nanoTime();

        if (leftNanos <= 0) {
            return 1;
        }

        long seconds = TimeUnit.NANOSECONDS.toSeconds(leftNanos);

        return (seconds <= 0) ? 1 : (int) Math.min(seconds, Integer.MAX_VALUE);
    }

}

