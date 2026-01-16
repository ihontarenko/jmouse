package org.jmouse.crawler.spi;

import org.jmouse.core.Verify;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class RPSPolitenessPolicy implements PolitenessPolicy {

    private final Duration                 interval;
    private final AtomicReference<Instant> atomicReference;

    public RPSPolitenessPolicy(double maxRps) {
        this(maxRps, new AtomicReference<>(Instant.EPOCH));
    }

    public RPSPolitenessPolicy(double maxRps, AtomicReference<Instant> atomicReference) {
        Verify.state(maxRps > 0.0, "maxRps must be > 0");
        this.interval = Duration.ofNanos((long) (1_000_000_000.0 / maxRps));
        this.atomicReference = Verify.nonNull(atomicReference, "atomicReference");
    }

    @Override
    public Instant notBefore(URI url, Instant instant) {
        if (instant == null) {
            return Instant.EPOCH;
        }

        while (true) {
            Instant previous = atomicReference.get();

            if (previous.isAfter(instant)) {
                return previous;
            }

            Instant reserveNext = instant.plus(interval);

            if (atomicReference.compareAndSet(previous, reserveNext)) {
                return instant;
            }
        }
    }
}
