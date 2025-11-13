package org.jmouse.tx;

import org.jmouse.core.Bits;

public enum TransactionState {

    NEW(1L),
    COMPLETED(2L),
    READ_ONLY(4L),
    NESTED(8L),
    ROLLBACK_ONLY(16L);

    private final long value;

    TransactionState(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public Bits getBitMask() {
        return Bits.of(value);
    }

}
