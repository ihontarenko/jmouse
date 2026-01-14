package org.jmouse.crawler.spi;

import org.jmouse.core.Verify;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PerHostMinDelayPolitenessPolicy implements PolitenessPolicy {

    private final Duration                       minDelay;
    private final ConcurrentMap<String, Instant> nextAllowed;

    public PerHostMinDelayPolitenessPolicy(Duration minDelay) {
        this(minDelay, new ConcurrentHashMap<>());
    }

    public PerHostMinDelayPolitenessPolicy(Duration minDelay, ConcurrentMap<String, Instant> nextAllowed) {
        this.minDelay = Verify.nonNull(minDelay, "minDelay");
        this.nextAllowed = Verify.nonNull(nextAllowed, "nextAllowed");
    }

    @Override
    public Instant notBefore(URI url, Instant now) {
        if (url == null || now == null) {
            return now;
        }

        String host = url.getHost();

        if (host == null || host.isBlank()) {
            return now;
        }

        final Holder holder = new Holder();

        nextAllowed.compute(host, (h, previousNextAllowed) -> {
            Instant nextAllowed = (previousNextAllowed == null) ? now : previousNextAllowed;

            // allowed now
            if (!nextAllowed.isAfter(now)) {
                holder.notBefore = now;                 // can go now
                return now.plus(minDelay);              // reserve next slot
            }

            // not allowed yet
            holder.notBefore = nextAllowed;                // must wait until nextAllowed
            return nextAllowed;                         // keep as-is
        });

        return holder.notBefore != null ? holder.notBefore : now;
    }

    private static final class Holder {
        Instant notBefore;
    }
}
