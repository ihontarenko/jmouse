package org.jmouse.core.throttle;

/**
 * ⏳ <b>RateLimiter</b> — sealed interface for token-bucket–based
 * rate limiting strategies.
 *
 * <p>Defines the contract for acquiring permits (tokens) that regulate
 * the flow of requests. Implementations differ in how tokens are refilled:</p>
 *
 * <ul>
 *   <li>🪣 {@link FixedRateTokenBucket} — refill at fixed rate (RPS-based).</li>
 *   <li>🪙 {@link SmoothTokenBucket} — continuous refill (capacity per period).</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * // Fixed-rate: 5 requests per second, burst capacity 10
 * RateLimiter limiter = RateLimiter.fixed(5.0, 10.0);
 *
 * // Smooth: 2 tokens per 1 second
 * RateLimiter limiter = RateLimiter.smooth(2, 1_000_000_000L);
 *
 * if (limiter.tryAcquire()) {
 *     // ✅ Allowed
 * } else {
 *     // ❌ Denied (rate limited)
 * }
 * }</pre>
 */
public sealed interface RateLimiter
        permits FixedRateTokenBucket, SmoothTokenBucket {

    /**
     * Attempts to consume a single permit.
     *
     * @return {@code true} if allowed, {@code false} otherwise.
     */
    boolean tryAcquire();

    /**
     * Attempts to consume multiple permits (sequentially).
     *
     * @param permits number of tokens requested (≥1)
     * @return {@code true} if all were granted, {@code false} otherwise
     */
    default boolean tryAcquire(int permits) {
        for (int i = 0; i < permits; i++) {
            if (!tryAcquire()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Factory method for a {@link FixedRateTokenBucket}.
     *
     * @param rps    average refill rate (tokens per second)
     * @param bursts maximum burst capacity
     * @return new {@link RateLimiter} instance
     */
    static RateLimiter fixed(double rps, double bursts) {
        return new FixedRateTokenBucket(rps, bursts);
    }

    /**
     * Factory method for a {@link SmoothTokenBucket}.
     *
     * @param capacity    maximum tokens in the bucket
     * @param nanoSeconds period in nanoseconds to refill the bucket fully
     * @return new {@link RateLimiter} instance
     */
    static RateLimiter smooth(long capacity, long nanoSeconds) {
        return new SmoothTokenBucket(capacity, nanoSeconds);
    }
}
