package org.jmouse.ai.provider;

import java.time.Duration;

/** Immutable HTTP admission policy. Response bytes count all delivered body bytes before decoding/JSON parsing. */
public record ChatHttpPolicy(Duration connectTimeout, Duration responseTimeout, int maximumResponseBytes) {

    public ChatHttpPolicy {
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
                || responseTimeout == null || responseTimeout.isZero() || responseTimeout.isNegative()
                || maximumResponseBytes < 1) {
            throw new IllegalArgumentException("Positive connection/response deadlines and response byte allowance are required.");
        }
        try {
            responseTimeout.toNanos();
        } catch (ArithmeticException excessive) {
            throw new IllegalArgumentException("Response deadline exceeds the supported finite timer range.", excessive);
        }
    }
}
