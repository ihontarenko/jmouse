package org.jmouse.crawler.spi;

import org.jmouse.core.Verify;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-host politeness policy that enforces a minimum delay between requests
 * to the same host. 🕊️
 *
 * <p>For each host, this policy keeps track of the next time at which a request
 * is allowed to execute. If a request arrives before that time, the policy
 * returns the stored instant; otherwise it returns {@code now} and advances
 * the host's next-allowed instant by {@code minDelay}.</p>
 *
 * <p>Thread-safety: this implementation is safe for concurrent use.
 * Host state is maintained in a {@link ConcurrentMap} and updated atomically
 * via {@link ConcurrentMap#compute(Object, java.util.function.BiFunction)}.</p>
 */
public final class PerHostMinDelayPolitenessPolicy implements PolitenessPolicy {

    private final Duration                       minDelay;
    private final ConcurrentMap<String, Instant> nextEligibleAtByHost;

    /**
     * Create the policy using an internal {@link ConcurrentHashMap} to store host state.
     *
     * @param minDelay minimum delay between requests to the same host
     */
    public PerHostMinDelayPolitenessPolicy(Duration minDelay) {
        this(minDelay, new ConcurrentHashMap<>());
    }

    /**
     * Create the policy using a caller-provided map (useful for tests or shared state).
     *
     * @param minDelay               minimum delay between requests to the same host
     * @param nextEligibleAtByHost   per-host "next eligible at" instants
     */
    public PerHostMinDelayPolitenessPolicy(Duration minDelay, ConcurrentMap<String, Instant> nextEligibleAtByHost) {
        this.minDelay = Verify.nonNull(minDelay, "minDelay");
        this.nextEligibleAtByHost = Verify.nonNull(nextEligibleAtByHost, "nextEligibleAtByHost");
    }

    /**
     * Return the earliest instant at which a request to the given URL becomes eligible.
     *
     * <p>Null URL or time is treated as immediately eligible (returns {@code now}).</p>
     *
     * @param url the request URL
     * @param now current time reference
     * @return earliest eligibility instant for this host
     */
    @Override
    public Instant eligibleAt(URI url, Instant now) {
        if (url == null || now == null) {
            return now;
        }

        String host = url.getHost();
        if (host == null || host.isBlank()) {
            return now;
        }

        // Holder used to return a computed value from the atomic 'compute' lambda.
        final Result result = new Result();

        nextEligibleAtByHost.compute(host, (h, previousNextEligibleAt) -> {
            Instant nextEligibleAt = (previousNextEligibleAt == null) ? now : previousNextEligibleAt;

            // If the host is already eligible, we allow now and advance the next window.
            if (!nextEligibleAt.isAfter(now)) {
                result.eligibleAt = now;
                return now.plus(minDelay);
            }

            // Otherwise, we must wait until the previously computed eligibility instant.
            result.eligibleAt = nextEligibleAt;
            return nextEligibleAt;
        });

        return (result.eligibleAt != null) ? result.eligibleAt : now;
    }

    private static final class Result {
        Instant eligibleAt;
    }
}
