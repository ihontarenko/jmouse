package org.jmouse.web.security;

/**
 * 🪣 A simple rate limiter based on the <b>Token Bucket</b> algorithm.
 * <p>
 * ✅ Idea: the "bucket" has a maximum {@code capacity} of tokens.
 * It refills at a constant rate {@code refillPerSec}. Each call to
 * {@link #tryAcquire()} attempts to consume one token. If a token
 * is available, the operation is allowed; otherwise it is denied
 * until more tokens are refilled.
 * </p>
 *
 * <p>
 * 🧵 Thread-safety: the method {@link #tryAcquire()} is synchronized.
 * </p>
 *
 * <p>
 * 🔧 Typical use case: limiting requests per second (RPS) when
 * accessing external OSINT sources (e.g. AbuseIPDB, ThreatFox,
 * Project Honey Pot) to avoid exceeding API limits and to smooth
 * traffic bursts.
 * </p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * // Allow up to 5 requests per second with a burst capacity of 10
 * TokenBucket bucket = new TokenBucket(5.0, 10.0);
 *
 * if (bucket.tryAcquire()) {
 *     // ✅ Allowed: call external reputation API
 *     callOsintReputationApi(ip);
 * } else {
 *     // ❌ Rate limit exceeded: apply fallback/queue/retry
 *     log.debug("Rate limit hit, delaying...");
 * }
 * }</pre>
 *
 * <h3>Terminology</h3>
 * <ul>
 *   <li><b>rps</b> — average refill rate (tokens per second).</li>
 *   <li><b>burst</b> — maximum instantaneous bucket size (capacity).</li>
 * </ul>
 */
public class TokenBucket {

    /**
     * Maximum number of tokens the bucket can hold.
     */
    private final double capacity;

    /**
     * Refill rate: tokens per second (RPS).
     */
    private final double refillPerSec;

    /**
     * Current token count.
     */
    private double tokens;

    /**
     * Last refill timestamp in nanoseconds.
     */
    private long lastNanos;

    /**
     * Constructs a new Token Bucket.
     *
     * @param rps   the desired average refill rate (tokens per second).
     * @param burst the maximum burst capacity; actual {@code capacity = max(burst, rps)}.
     *
     *              <p>
     *              📌 Tip: If unsure about {@code burst}, pick 2–3× the {@code rps}
     *              to allow short bursts without sustained overload.
     *              </p>
     */
    public TokenBucket(double rps, double burst) {
        this.capacity = Math.max(burst, rps);
        this.refillPerSec = rps;
        this.tokens = capacity;
        this.lastNanos = System.nanoTime();
    }

    /**
     * Attempts to acquire 1 token.
     * <p>
     * Algorithm steps:
     * <ol>
     *   <li>Compute elapsed time since last refill.</li>
     *   <li>Replenish tokens: {@code tokens += refillPerSec * delta},
     *       capped at {@code capacity}.</li>
     *   <li>If {@code tokens >= 1}, consume one token and return {@code true}.</li>
     *   <li>Otherwise return {@code false}.</li>
     * </ol>
     *
     * @return {@code true} if a token was available and consumed,
     * {@code false} if the request must wait.
     */
    public synchronized boolean tryAcquire() {
        long   now   = System.nanoTime();
        double delta = (now - lastNanos) / 1_000_000_000.0;

        // Refill tokens (capped at capacity)
        tokens = Math.min(capacity, tokens + refillPerSec * delta);
        lastNanos = now;

        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }

        return false;
    }

}
