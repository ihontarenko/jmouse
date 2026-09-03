package org.jmouse.money.exception;

import org.jmouse.money.CurrencyCode;

/**
 * 🚫 Two amounts of different currencies were treated as one.
 *
 * <p>⚠️ A programming error rather than a runtime condition — see {@code Money#plus}. It carries both
 * currencies because the interesting part of the message is <em>which two</em>: a stack trace saying
 * "currency mismatch" sends somebody looking through every amount in the call, and one saying
 * "UAH and USD" usually names the bug on sight.</p>
 */
public class CurrencyMismatchException extends RuntimeException {

    private final transient CurrencyCode expected;
    private final transient CurrencyCode actual;

    public CurrencyMismatchException(CurrencyCode expected, CurrencyCode actual) {
        super("Cannot combine " + expected + " with " + actual + " — they are different currencies");
        this.expected = expected;
        this.actual   = actual;
    }

    /** 💱 The currency being added to. */
    public CurrencyCode getExpected() {
        return expected;
    }

    /** 💱 The currency that arrived instead. */
    public CurrencyCode getActual() {
        return actual;
    }
}
