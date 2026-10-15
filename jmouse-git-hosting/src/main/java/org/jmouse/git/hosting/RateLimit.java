package org.jmouse.git.hosting;

import java.net.http.HttpHeaders;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * The rate-limit state a hosting reported with a response. Every field is {@code null} when the hosting did not
 * send it; nothing is guessed. GitHub sends {@code x-ratelimit-*}, GitLab sends {@code ratelimit-*}, and both may
 * send {@code retry-after} when they refuse.
 *
 * @param limit      requests allowed in the current window
 * @param remaining  requests left in the current window
 * @param resetsAt   when the window resets
 * @param retryAfter how long the hosting asked the caller to wait
 */
public record RateLimit(Long limit, Long remaining, Instant resetsAt, Duration retryAfter) {

    /** A response that carried no rate-limit headers. */
    public static final RateLimit NOT_REPORTED = new RateLimit(null, null, null, null);

    /** Whether the window is used up. */
    public boolean exhausted() {
        return remaining != null && remaining == 0;
    }

    /** Whether the hosting asked the caller to wait before trying again. */
    public boolean asksToWait() {
        return exhausted() || retryAfter != null;
    }

    /** Reads whichever rate-limit headers the response carries; a malformed value counts as not sent. */
    public static RateLimit of(HttpHeaders headers) {
        if (headers == null) {
            return NOT_REPORTED;
        }
        Long limit     = number(headers, "x-ratelimit-limit", "ratelimit-limit");
        Long remaining = number(headers, "x-ratelimit-remaining", "ratelimit-remaining");
        Long reset     = number(headers, "x-ratelimit-reset", "ratelimit-reset");
        Long wait      = number(headers, "retry-after");
        return new RateLimit(limit, remaining,
                reset == null ? null : Instant.ofEpochSecond(reset),
                wait == null ? null : Duration.ofSeconds(wait));
    }

    private static Long number(HttpHeaders headers, String... names) {
        for (String name : names) {
            Optional<String> value = headers.firstValue(name);
            if (value.isPresent()) {
                try {
                    long parsed = Long.parseLong(value.get().strip());
                    return parsed < 0 ? null : parsed;
                } catch (NumberFormatException malformed) {
                    return null;
                }
            }
        }
        return null;
    }
}
