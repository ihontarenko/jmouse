package org.jmouse.crawler.spi;

import org.jmouse.core.Verify;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
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
    public Instant notBefore(URI url, Instant instant) {
        if (url == null || instant == null) {
            return instant;
        }

        String host = url.getHost();

        if (host == null || host.isBlank()) {
            return instant;
        }

        final Holder holder = new Holder();

        nextAllowed.compute(host, (h, previousNextAllowed) -> {
            Instant nextAllowed = (previousNextAllowed == null) ? instant : previousNextAllowed;

            if (!nextAllowed.isAfter(instant)) {
                holder.notBefore = instant;
                return instant.plus(minDelay);
            }

            holder.notBefore = nextAllowed;
            return nextAllowed;
        });

        return holder.notBefore != null ? holder.notBefore : instant;
    }

    private static final class Holder {
        Instant notBefore;
    }
}
