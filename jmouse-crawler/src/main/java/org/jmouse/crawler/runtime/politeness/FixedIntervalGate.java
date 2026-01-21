package org.jmouse.crawler.runtime.politeness;

import org.jmouse.core.Verify;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class FixedIntervalGate implements TimeGate {

    private final Duration                 interval;
    private final AtomicReference<Instant> next = new AtomicReference<>(Instant.EPOCH);

    public FixedIntervalGate(Duration interval) {
        this.interval = Verify.nonNull(interval, "interval");
    }

    @Override
    public Instant eligibleAt(Instant now) {
        while (true) {
            Instant previous = next.get();
            Instant allowed  = previous.isAfter(now) ? previous : now;
            Instant reserve  = allowed.plus(interval);

            if (next.compareAndSet(previous, reserve)) {
                return allowed;
            }
        }
    }
}
