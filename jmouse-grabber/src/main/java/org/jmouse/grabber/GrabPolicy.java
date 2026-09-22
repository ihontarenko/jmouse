package org.jmouse.grabber;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jmouse.core.proxy.interceptor.RetryInterceptor;

/**
 * ⚙️ How a run behaves — everything the brief lists, in one record.
 *
 * <p>⚠️ <b>It is declared once and narrowed per route, never widened.</b> A route may ask to go
 * slower or to give up sooner; a route that could raise the run's concurrency would make the run's own
 * figure a lie, and the figure a person reads on the builder has to be the truth about the whole run.
 * {@link #narrowedBy(GrabPolicy)} is where that rule lives, so no caller has to remember it.</p>
 *
 * @param concurrency  how many pages may be in flight at once
 * @param minimumDelay the shortest wait between two requests to the same host
 * @param maximumDelay the longest — the actual wait is drawn between the two, so a run does not
 *                     arrive on a clock
 * @param timeout      how long one exchange may take
 * @param retries      how many attempts a failing page gets, in total
 * @param retryBackoff how long to wait before attempt <i>n</i>
 * @param maximumDepth how many follows from a seed, or {@code null} for no limit
 * @param maximumPages how many pages the whole run may visit, or {@code null} for no limit
 * @param userAgent    what to call ourselves
 * @param headers      headers sent with every request
 * @param requestsPerSecond a hard ceiling per host, or {@code null} for none
 * @param burst        how many requests may go at once before the ceiling applies
 */
public record GrabPolicy(
        int concurrency,
        Duration minimumDelay,
        Duration maximumDelay,
        Duration timeout,
        int retries,
        RetryInterceptor.Backoff retryBackoff,
        Integer maximumDepth,
        Integer maximumPages,
        String userAgent,
        Map<String, String> headers,
        Double requestsPerSecond,
        Double burst
) {

    /**
     * 🪪 What a run calls itself when nobody said.
     *
     * <p>Naming the library honestly is deliberate: a site owner reading their logs should be able to
     * tell what visited them, and a grabber pretending to be a browser by default would make that
     * decision on the caller's behalf.</p>
     */
    public static final String DEFAULT_USER_AGENT = "jMouse-Grabber/1.0";

    public GrabPolicy {
        if (concurrency < 1) {
            throw new IllegalArgumentException("Concurrency must be at least 1, got " + concurrency);
        }

        if (retries < 0) {
            throw new IllegalArgumentException("Retries cannot be negative, got " + retries);
        }

        minimumDelay = minimumDelay == null ? Duration.ZERO : minimumDelay;
        maximumDelay = maximumDelay == null || maximumDelay.compareTo(minimumDelay) < 0 ? minimumDelay : maximumDelay;
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        retryBackoff = retryBackoff == null ? RetryInterceptor.Backoff.exponential(500L, 30_000L) : retryBackoff;
        userAgent = userAgent == null ? DEFAULT_USER_AGENT : userAgent;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    /**
     * ⚙️ Sensible for a run nobody has thought about yet: one page at a time, half a second apart,
     * three attempts.
     *
     * <p>⚠️ The defaults are deliberately <b>polite rather than fast</b>. A grabber whose out-of-the-box
     * behaviour hammers a site is one whose first run gets somebody's address blocked, and the figure
     * that makes it fast is one line away.</p>
     */
    public static GrabPolicy defaults() {
        return new GrabPolicy(
                1,
                Duration.ofMillis(500), Duration.ofMillis(500),
                Duration.ofSeconds(30),
                3, RetryInterceptor.Backoff.exponential(500L, 30_000L),
                null, null,
                DEFAULT_USER_AGENT, Map.of(),
                null, null
        );
    }

    /**
     * 🛠️ A builder, for the fluent assembly.
     */
    public static Builder builder() {
        return new Builder(defaults());
    }

    /**
     * 🛠️ A builder starting from this policy.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * 🔒 This policy, narrowed by another — the stricter of the two wins on every field.
     *
     * <p>⚠️ <b>Narrowed, not merged.</b> A route asking for four in flight when the run allows one
     * gets one. A route asking for a second between requests when the run asks for 300ms gets the
     * second. The direction is the point: a route can slow a run down and can never speed it up.</p>
     */
    public GrabPolicy narrowedBy(GrabPolicy other) {
        if (other == null) {
            return this;
        }

        Map<String, String> mergedHeaders = new LinkedHashMap<>(headers);

        mergedHeaders.putAll(other.headers);

        return new GrabPolicy(
                Math.min(concurrency, other.concurrency),
                longer(minimumDelay, other.minimumDelay),
                longer(maximumDelay, other.maximumDelay),
                shorter(timeout, other.timeout),
                Math.min(retries, other.retries),
                other.retryBackoff == null ? retryBackoff : other.retryBackoff,
                smaller(maximumDepth, other.maximumDepth),
                smaller(maximumPages, other.maximumPages),
                other.userAgent == null ? userAgent : other.userAgent,
                mergedHeaders,
                smaller(requestsPerSecond, other.requestsPerSecond),
                smaller(burst, other.burst)
        );
    }

    /**
     * ⏱️ A wait drawn between the minimum and the maximum.
     */
    public Duration nextDelay(java.util.random.RandomGenerator random) {
        if (maximumDelay.compareTo(minimumDelay) <= 0) {
            return minimumDelay;
        }

        long spread = maximumDelay.toMillis() - minimumDelay.toMillis();

        return Duration.ofMillis(minimumDelay.toMillis() + random.nextLong(spread + 1));
    }

    public boolean hasRateLimit() {
        return requestsPerSecond != null && requestsPerSecond > 0;
    }

    public boolean allowsDepth(int depth) {
        return maximumDepth == null || depth <= maximumDepth;
    }

    public boolean allowsAnotherPage(long visited) {
        return maximumPages == null || visited < maximumPages;
    }

    private static Duration longer(Duration first, Duration second) {
        return first.compareTo(second) >= 0 ? first : second;
    }

    private static Duration shorter(Duration first, Duration second) {
        return first.compareTo(second) <= 0 ? first : second;
    }

    private static Integer smaller(Integer first, Integer second) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return Math.min(first, second);
    }

    private static Double smaller(Double first, Double second) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return Math.min(first, second);
    }

    /**
     * 🛠️ The fluent half, so the assembly reads as prose rather than as a twelve-argument constructor.
     */
    public static final class Builder {

        private int                      concurrency;
        private Duration                 minimumDelay;
        private Duration                 maximumDelay;
        private Duration                 timeout;
        private int                      retries;
        private RetryInterceptor.Backoff retryBackoff;
        private Integer                  maximumDepth;
        private Integer                  maximumPages;
        private String                   userAgent;
        private Map<String, String>      headers;
        private Double                   requestsPerSecond;
        private Double                   burst;

        private Builder(GrabPolicy from) {
            this.concurrency = from.concurrency;
            this.minimumDelay = from.minimumDelay;
            this.maximumDelay = from.maximumDelay;
            this.timeout = from.timeout;
            this.retries = from.retries;
            this.retryBackoff = from.retryBackoff;
            this.maximumDepth = from.maximumDepth;
            this.maximumPages = from.maximumPages;
            this.userAgent = from.userAgent;
            this.headers = new LinkedHashMap<>(from.headers);
            this.requestsPerSecond = from.requestsPerSecond;
            this.burst = from.burst;
        }

        /**
         * 🧵 How many pages may be in flight at once.
         */
        public Builder concurrency(int value) {
            this.concurrency = value;
            return this;
        }

        /**
         * ⏱️ Exactly this long between requests to one host.
         */
        public Builder delay(Duration value) {
            this.minimumDelay = value;
            this.maximumDelay = value;
            return this;
        }

        /**
         * ⏱️ Somewhere between these, drawn per request — which is what stops a run arriving on a
         * clock.
         */
        public Builder delay(Duration minimum, Duration maximum) {
            this.minimumDelay = minimum;
            this.maximumDelay = maximum;
            return this;
        }

        public Builder timeout(Duration value) {
            this.timeout = value;
            return this;
        }

        /**
         * ♻️ How many attempts a failing page gets, in total.
         */
        public Builder retries(int value) {
            this.retries = value;
            return this;
        }

        /**
         * ♻️ How long to wait before the next attempt.
         *
         * <p>The library's own — {@code Backoff.fixed}, {@code exponential}, {@code jitter} — rather
         * than one written here.</p>
         */
        public Builder retryBackoff(RetryInterceptor.Backoff value) {
            this.retryBackoff = value;
            return this;
        }

        public Builder maximumDepth(int value) {
            this.maximumDepth = value;
            return this;
        }

        public Builder maximumPages(int value) {
            this.maximumPages = value;
            return this;
        }

        public Builder userAgent(String value) {
            this.userAgent = value;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        /**
         * 🚦 A hard ceiling per host, on top of the delay.
         *
         * @param perSecond the sustained rate
         * @param bursts    how many may go at once before it applies
         */
        public Builder rateLimit(double perSecond, double bursts) {
            this.requestsPerSecond = perSecond;
            this.burst = bursts;
            return this;
        }

        public GrabPolicy build() {
            return new GrabPolicy(
                    concurrency, minimumDelay, maximumDelay, timeout, retries, retryBackoff,
                    maximumDepth, maximumPages, userAgent, headers, requestsPerSecond, burst);
        }
    }

}
