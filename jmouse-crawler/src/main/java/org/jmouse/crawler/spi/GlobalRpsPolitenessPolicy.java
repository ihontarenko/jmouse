package org.jmouse.crawler.spi;

import org.jmouse.core.Verify;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class GlobalRpsPolitenessPolicy implements PolitenessPolicy {

    private final Duration                 interval;
    private final AtomicReference<Instant> nextAllowed;

    public GlobalRpsPolitenessPolicy(double maxRps) {
        this(maxRps, new AtomicReference<>(Instant.EPOCH));
    }

    public GlobalRpsPolitenessPolicy(double maxRps, AtomicReference<Instant> nextAllowed) {
        Verify.state(maxRps > 0.0, "maxRps must be > 0");
        this.interval = Duration.ofNanos((long) (1_000_000_000.0 / maxRps));
        this.nextAllowed = Verify.nonNull(nextAllowed, "nextAllowed");
    }

    @Override
    public Instant notBefore(URI url, Instant now) {
        if (now == null) return Instant.EPOCH;

        while (true) {
            Instant previous    = nextAllowed.get();
            Instant allowed     = previous.isAfter(now) ? previous : now;
            Instant reserveNext = allowed.plus(interval);

            if (nextAllowed.compareAndSet(previous, reserveNext)) {
                return allowed;
            }
        }
    }
}
