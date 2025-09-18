package org.jmouse.core.throttle;

/**
 * 🪣 <b>Fixed-Rate Token Bucket</b> rate limiter.
 *
 * <p>Refills tokens at a fixed average rate (RPS, tokens per second).
 * Each call to {@link #tryAcquire()} attempts to consume one token:
 * <ul>
 *   <li>✅ If a token is available — request is allowed.</li>
 *   <li>❌ If empty — request is denied until refill.</li>
 * </ul>
 * </p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>🔧 Configurable <b>rps</b> (tokens per second).</li>
 *   <li>💥 Configurable <b>burst</b> capacity (max bucket size).</li>
 *   <li>🧵 Thread-safety via {@code synchronized} method.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * // Allow up to 5 requests per second with a burst capacity of 10
 * RateLimiter limiter = new FixedRateTokenBucket(5.0, 10.0);
 *
 * if (limiter.tryAcquire()) {
 *     // ✅ Allowed: call external API
 *     callExternalApi();
 * } else {
 *     // ❌ Denied: rate limit exceeded
 *     log.debug("Rate limit hit, delaying...");
 * }
 * }</pre>
 *
 * <h3>Terminology</h3>
 * <ul>
 *   <li><b>rps</b> — average refill rate (tokens per second).</li>
 *   <li><b>burst</b> — maximum instantaneous capacity.</li>
 * </ul>
 */
public final class FixedRateTokenBucket implements RateLimiter {

    /**
     * 🚰 Maximum number of tokens the bucket can hold (burst size).
     */
    private final double capacity;

    /**
     * ⏱ Average refill rate (tokens per second).
     */
    private final double refillPerSecond;

    /**
     * 🎟 Current number of tokens.
     */
    private double tokens;

    /**
     * 🕒 Last refill timestamp in nanoseconds.
     */
    private long lastNanos;

    /**
     * Constructs a new {@link FixedRateTokenBucket}.
     *
     * @param rps   desired average refill rate (tokens per second).
     * @param burst maximum burst capacity; effective capacity = {@code max(burst, rps)}.
     *
     * <p>📌 Tip: choose burst ≈ 2–3× rps to allow short bursts
     * while preserving long-term average rate.</p>
     */
    public FixedRateTokenBucket(double rps, double burst) {
        this.capacity = Math.max(burst, rps);
        this.refillPerSecond = rps;
        this.tokens = capacity;
        this.lastNanos = System.nanoTime();
    }

    /**
     * Attempts to acquire one token.
     *
     * <p>Steps:
     * <ol>
     *   <li>Compute elapsed time since last refill.</li>
     *   <li>Refill tokens: {@code tokens += refillPerSecond * deltaSeconds},
     *       capped at {@code capacity}.</li>
     *   <li>If {@code tokens >= 1} → consume one and return {@code true}.</li>
     *   <li>Else return {@code false}.</li>
     * </ol>
     * </p>
     *
     * @return {@code true} if a token was consumed, {@code false} otherwise
     */
    @Override
    public synchronized boolean tryAcquire() {
        long   now   = System.nanoTime();
        double delta = (now - lastNanos) / 1_000_000_000.0;

        // 🔄 Refill tokens
        tokens = Math.min(capacity, tokens + refillPerSecond * delta);
        lastNanos = now;

        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }
}
