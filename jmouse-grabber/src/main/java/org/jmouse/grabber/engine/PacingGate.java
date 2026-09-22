package org.jmouse.grabber.engine;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.random.RandomGenerator;

import org.jmouse.core.throttle.RateLimiter;
import org.jmouse.grabber.GrabPolicy;

/**
 * 🚦 What "politeness" actually is.
 *
 * <p>Two mechanisms, both per host, because a run spanning three sites should not throttle all three
 * together and a site's patience is its own:</p>
 * <ul>
 *   <li>a <b>minimum wait</b> between two requests, drawn from the policy's range so the run does not
 *       arrive on a metronome;</li>
 *   <li>a <b>rate limiter</b> — {@link org.jmouse.core.throttle.RateLimiter}, the library's own token
 *       bucket rather than one written here — as a hard ceiling.</li>
 * </ul>
 *
 * <p>⚠️ <b>This is the only coordination point in a run</b>, which is exactly why virtual threads are
 * the right concurrency model: a thread waiting here costs nothing, so there is no reason to bound the
 * workers below what the policy asks for.</p>
 */
public final class PacingGate {

    private final GrabPolicy      policy;
    private final RandomGenerator random;

    private final Map<String, Long>        lastRequest = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> limiters    = new ConcurrentHashMap<>();
    private final Map<String, Object>      hostLocks   = new ConcurrentHashMap<>();

    public PacingGate(GrabPolicy policy, RandomGenerator random) {
        this.policy = policy;
        this.random = random;
    }

    /**
     * 🚦 Waits until this host may be asked again, then returns.
     *
     * <p>Blocks the calling thread. On a virtual thread that is free; on a platform thread it is the
     * behaviour a caller asked for by configuring a delay.</p>
     */
    public void pass(URI address) {
        String host = hostOf(address);

        waitForTurn(host);
        waitForToken(host);
    }

    /**
     * ⏱️ Holds the host's own lock while it waits, so two workers on the same host queue behind each
     * other rather than both reading the same "last request" and both going at once.
     */
    private void waitForTurn(String host) {
        Duration delay = policy.nextDelay(random);

        if (delay.isZero() || delay.isNegative()) {
            return;
        }

        Object lock = hostLocks.computeIfAbsent(host, key -> new Object());

        synchronized (lock) {
            Long previous = lastRequest.get(host);

            if (previous != null) {
                parkUntil(previous + delay.toNanos());
            }

            lastRequest.put(host, System.nanoTime());
        }
    }

    /**
     * ⏳ Parks until this moment has actually passed.
     *
     * <p>⚠️ <b>A single {@code parkNanos} is not enough and the shortfall is small enough to look like
     * noise.</b> It is documented to return early, and on this platform the timer granularity alone
     * costs several milliseconds — a gate asked for 200ms measured 192ms, which reads as jitter right
     * up until it is the reason a site starts refusing the run. So it loops until the clock agrees.</p>
     */
    private void parkUntil(long deadline) {
        long remaining = deadline - System.nanoTime();

        while (remaining > 0) {
            LockSupport.parkNanos(remaining);
            remaining = deadline - System.nanoTime();
        }
    }

    /**
     * 🪣 Spins on the host's token bucket until it yields.
     *
     * <p>The bucket answers yes or no rather than handing out a wait, so the only thing to do with a
     * no is to sleep a little and ask again. A tenth of the period is short enough not to waste the
     * allowance and long enough not to be a busy loop.</p>
     */
    private void waitForToken(String host) {
        if (!policy.hasRateLimit()) {
            return;
        }

        RateLimiter limiter = limiters.computeIfAbsent(
                host, key -> RateLimiter.fixed(policy.requestsPerSecond(), burst()));

        long pause = (long) (1_000_000_000L / policy.requestsPerSecond() / 10);

        while (!limiter.tryAcquire()) {
            LockSupport.parkNanos(Math.max(pause, 1_000_000L));
        }
    }

    private double burst() {
        return policy.burst() == null ? Math.max(1.0, policy.requestsPerSecond()) : policy.burst();
    }

    /**
     * 🏠 The host a limiter is kept for.
     *
     * <p>Host and port together, so a development site on two ports is two hosts, and lower-cased
     * because a host name is case-insensitive and a policy keyed on case would silently double every
     * allowance.</p>
     */
    static String hostOf(URI address) {
        String host = address.getHost();

        if (host == null) {
            return address.toString();
        }

        String key = host.toLowerCase(Locale.ROOT);

        return address.getPort() < 0 ? key : key + ":" + address.getPort();
    }

}
