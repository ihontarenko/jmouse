package org.jmouse.crawler.runtime.politeness;

import org.jmouse.core.Verify;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class FixedIntervalGate implements TimeGate {

    private final Duration                 delay;
    private final AtomicReference<Instant> next = new AtomicReference<>();

    public FixedIntervalGate(Duration delay) {
        this.delay = Verify.nonNull(delay, "delay");
    }

    public Instant acquire(Instant now) {
        while (true) {
            Instant previous = next.get();
            Instant eligible = (previous == null) ? now : previous;

            if (eligible.isAfter(now)) {
                return eligible;
            }

            Instant next = now.plus(delay);

            if (this.next.compareAndSet(previous, next)) {
                return now;
            }
        }
    }
}
