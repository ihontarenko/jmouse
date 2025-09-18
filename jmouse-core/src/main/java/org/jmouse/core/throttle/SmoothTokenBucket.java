package org.jmouse.core.throttle;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 🪙 <b>Smooth Token Bucket</b> rate limiter.
 *
 * <p>Refills tokens continuously at a constant rate, based on nanoseconds.
 * Each call to {@link #tryAcquire()} attempts to consume one token.
 * If a token is available → ✅ allowed, otherwise → ❌ denied.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>🔧 Configurable {@code capacity} (max burst size).</li>
 *   <li>⏱ Refill rate defined as {@code capacity / periodNanos}.</li>
 *   <li>🧵 Thread-safety via {@link ReentrantLock}.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * // Allow 2 requests per second (capacity=2, period=1s)
 * RateLimiter limiter = new SmoothTokenBucket(2, 1_000_000_000L);
 *
 * if (limiter.tryAcquire()) {
 *     // ✅ allowed
 * } else {
 *     // ❌ rate-limited
 * }
 * }</pre>
 */
public final class SmoothTokenBucket implements RateLimiter {

    /**
     * 🚰 Maximum number of tokens the bucket can hold (burst size).
     */
    private final long capacity;

    /**
     * ⏱ Refill speed: tokens per nanosecond.
     */
    private final double refillPerNano;

    /**
     * 🔒 Lock for thread-safety.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 🎟 Current number of tokens.
     */
    private double tokens;

    /**
     * 🕒 Timestamp of last refill in nanoseconds.
     */
    private long lastRefill;

    /**
     * Constructs a new {@link SmoothTokenBucket}.
     *
     * @param capacity    maximum tokens (burst size).
     * @param nanoSeconds period in nanoseconds over which the bucket refills fully.
     *
     * <p>Example: {@code new SmoothTokenBucket(2, 1_000_000_000)} ⇒ 2 tokens per second.</p>
     */
    public SmoothTokenBucket(long capacity, long nanoSeconds) {
        this.capacity = capacity;
        this.tokens = capacity;
        this.refillPerNano = (double) capacity / (double) nanoSeconds;
        this.lastRefill = System.nanoTime();
    }

    /**
     * Attempts to acquire one token.
     *
     * @return {@code true} if a token was available and consumed;
     *         {@code false} if the bucket was empty.
     */
    @Override
    public boolean tryAcquire() {
        lock.lock();
        try {
            final long now = System.nanoTime();
            refill(now);

            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                return true;
            }

            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 🔄 Replenishes tokens based on elapsed nanoseconds since last refill.
     *
     * @param now current time in nanoseconds
     */
    private void refill(long now) {
        double delta = (now - lastRefill) * refillPerNano;
        tokens = Math.min(capacity, tokens + delta);
        lastRefill = now;
    }
}
