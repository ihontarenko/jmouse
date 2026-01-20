package org.jmouse.crawler.spi;

import org.jmouse.core.Verify;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global RPS-based {@link PolitenessPolicy} that enforces a maximum
 * number of requests per second across all URLs. ⏱️
 *
 * <p>{@code RPSPolitenessPolicy} implements a simple reservation-based
 * rate limiter. Each eligibility check reserves the next execution slot
 * by advancing a shared timestamp by a fixed interval derived from
 * {@code maxRps}.</p>
 *
 * <p>Semantics:</p>
 * <ul>
 *   <li>If the previously reserved instant is in the future, callers must wait
 *       until that instant.</li>
 *   <li>If the caller arrives at or after the reserved instant, it becomes
 *       immediately eligible and advances the reservation window.</li>
 * </ul>
 *
 * <p>This policy is:</p>
 * <ul>
 *   <li>lock-free (CAS-based)</li>
 *   <li>safe for concurrent use</li>
 *   <li>globally scoped (not per-host)</li>
 * </ul>
 *
 * <p>Typical use cases include:</p>
 * <ul>
 *   <li>global crawl throttling</li>
 *   <li>protecting upstream bandwidth</li>
 *   <li>benchmarking under controlled request rates</li>
 * </ul>
 */
public final class RPSPolitenessPolicy implements PolitenessPolicy {

    /**
     * Fixed interval between two consecutive allowed requests.
     * Derived as {@code 1s / maxRps}.
     */
    private final Duration interval;

    /**
     * Holds the next globally reserved eligibility instant.
     */
    private final AtomicReference<Instant> nextEligibleAt;

    /**
     * Create a global RPS politeness policy.
     *
     * @param maxRps maximum allowed requests per second (must be {@code > 0})
     */
    public RPSPolitenessPolicy(double maxRps) {
        this(maxRps, new AtomicReference<>(Instant.EPOCH));
    }

    /**
     * Create a global RPS politeness policy with a caller-provided state holder.
     *
     * <p>This constructor is primarily intended for testing or for sharing
     * rate-limiting state across multiple policy instances.</p>
     *
     * @param maxRps         maximum allowed requests per second (must be {@code > 0})
     * @param nextEligibleAt atomic holder of the next reserved instant
     */
    public RPSPolitenessPolicy(double maxRps, AtomicReference<Instant> nextEligibleAt) {
        Verify.state(maxRps > 0.0, "maxRps must be > 0");
        this.interval = Duration.ofNanos((long) (1_000_000_000.0 / maxRps));
        this.nextEligibleAt = Verify.nonNull(nextEligibleAt, "nextEligibleAt");
    }

    /**
     * Determine the earliest instant at which a request becomes eligible
     * under the global RPS limit.
     *
     * <p>The {@code url} parameter is ignored, as this policy is global
     * rather than per-host.</p>
     *
     * <p>This method uses a CAS loop to reserve the next execution slot
     * atomically.</p>
     *
     * @param url ignored
     * @param now current time reference
     * @return earliest eligibility instant for execution
     */
    @Override
    public Instant eligibleAt(URI url, Instant now) {
        if (now == null) {
            return Instant.EPOCH;
        }

        while (true) {
            Instant previous = nextEligibleAt.get();

            // Someone already reserved a future slot → must wait.
            if (previous.isAfter(now)) {
                return previous;
            }

            // Reserve the next slot starting from now.
            Instant reserveNext = now.plus(interval);

            if (nextEligibleAt.compareAndSet(previous, reserveNext)) {
                return now;
            }
        }
    }
}
