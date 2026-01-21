package org.jmouse.crawler.runtime.state.persistence;

import java.time.Duration;

public sealed interface Durability permits Durability.Sync, Durability.Batched, Durability.Async {

    static Durability sync() {
        return new Sync();
    }

    static Durability batched(int maxRecords, Duration maxDelay) {
        return new Batched(maxRecords, maxDelay);
    }

    static Durability async(Duration flushInterval) {
        return new Async(flushInterval);
    }

    record Sync() implements Durability {
    }

    record Batched(int maxRecords, Duration maxDelay) implements Durability {
        public Batched {
            if (maxRecords < 1) {
                throw new IllegalArgumentException("maxRecords must be >= 1");
            }
            if (maxDelay == null || maxDelay.isNegative() || maxDelay.isZero()) {
                throw new IllegalArgumentException("maxDelay must be > 0");
            }
        }
    }

    record Async(Duration flushInterval) implements Durability {
        public Async {
            if (flushInterval == null || flushInterval.isNegative() || flushInterval.isZero()) {
                throw new IllegalArgumentException("flushInterval must be > 0");
            }
        }
    }
}
