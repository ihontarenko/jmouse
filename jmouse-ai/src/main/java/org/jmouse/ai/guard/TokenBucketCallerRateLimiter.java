package org.jmouse.ai.guard;

import org.jmouse.ai.spi.CallerRateLimiter;
import org.jmouse.core.throttle.RateLimiter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One smooth token bucket per caller, held in this process.
 *
 * <p>Built over {@code jmouse-core}'s {@link RateLimiter} rather than reimplementing the arithmetic:
 * what this adds is the key, an eviction bound and the vocabulary the refusal is written in. A smooth
 * bucket rather than a fixed-rate one because <em>"the allowance refills continuously"</em> is a
 * sentence a throttled caller can act on, where a caller told it may retry at the top of the next
 * window will spend the rest of the window trying.
 *
 * <p>⚠️ <strong>Bounded, because the map is otherwise the leak.</strong> A caller identifier arrives
 * from outside; a store keyed by one with no bound grows for as long as identifiers keep being new.
 * Past {@link #capacity} the whole map is cleared rather than evicted one at a time — a caller whose
 * bucket is discarded gets a fresh full one, which is the permissive direction, and this limiter
 * guards against a loop rather than against an adversary. A deployment that needs the other direction
 * implements {@link CallerRateLimiter} over a shared cache.
 */
public final class TokenBucketCallerRateLimiter implements CallerRateLimiter {

    /** Upper bound on tracked callers, so the bucket map cannot itself become the problem. */
    private static final int DEFAULT_CAPACITY = 10_000;

    private final Map<String, RateLimiter> buckets = new ConcurrentHashMap<>();
    private final long                     allowance;
    private final Duration                 refillPeriod;
    private final int                      capacity;

    /**
     * @param allowance    calls one caller may burst before it is throttled
     * @param refillPeriod time to refill an empty bucket back to that allowance
     */
    public TokenBucketCallerRateLimiter(long allowance, Duration refillPeriod, int capacity) {
        this.allowance    = allowance;
        this.refillPeriod = refillPeriod;
        this.capacity     = capacity;
    }

    public TokenBucketCallerRateLimiter(long allowance, Duration refillPeriod) {
        this(allowance, refillPeriod, DEFAULT_CAPACITY);
    }

    /** Sixty calls a minute — enough for a working conversation, obviously not enough for a loop. */
    public static TokenBucketCallerRateLimiter defaults() {
        return new TokenBucketCallerRateLimiter(60, Duration.ofMinutes(1));
    }

    @Override
    public boolean tryAcquire(String callerId) {
        if (buckets.size() >= capacity) {
            buckets.clear();
        }

        return buckets
                .computeIfAbsent(callerId, tracked -> RateLimiter.smooth(allowance, refillPeriod.toNanos()))
                .tryAcquire();
    }

    @Override
    public String describeAllowance() {
        return allowance + " calls per " + refillPeriod.toSeconds()
             + " seconds, refilling continuously";
    }
}
