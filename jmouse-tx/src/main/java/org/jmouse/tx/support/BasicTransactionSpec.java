package org.jmouse.tx.support;

import org.jmouse.tx.IsolationLevel;
import org.jmouse.tx.PropagationPolicy;
import org.jmouse.tx.TransactionSpecification;

public final class BasicTransactionSpec implements TransactionSpecification {

    private final PropagationPolicy propagation;
    private final IsolationLevel    isolation;
    private final int               timeout;
    private final boolean           readOnly;
    private final String            name;

    public BasicTransactionSpec(
            PropagationPolicy propagation, IsolationLevel isolation, int timeout, boolean readOnly, String name
    ) {
        this.name = name;
        this.propagation = propagation;
        this.isolation = isolation;
        this.timeout = Math.max(0, timeout);
        this.readOnly = readOnly;
    }

    // handy factory methods
    public static BasicTransactionSpec required() {
        return new BasicTransactionSpec(PropagationPolicy.REQUIRED, IsolationLevel.DEFAULT, 0, false, null);
    }

    @Override
    public PropagationPolicy getPropagation() {
        return propagation;
    }

    @Override
    public IsolationLevel getIsolation() {
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
