package org.jmouse.transaction.infrastructure;

import org.jmouse.transaction.TransactionIsolation;

public record TransactionAttributes(TransactionIsolation isolation, boolean readOnly, long deadlineNanos) {
    public boolean hasTimeout() {
        return deadlineNanos > 0;
    }
}
