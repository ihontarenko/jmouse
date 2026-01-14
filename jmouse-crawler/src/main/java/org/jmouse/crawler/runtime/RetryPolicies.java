package org.jmouse.crawler.runtime;

import org.jmouse.crawler.spi.RetryDecision;
import org.jmouse.crawler.spi.RetryPolicy;
import org.jmouse.core.Verify;

import java.time.Duration;
import java.time.Instant;

public final class RetryPolicies {

    private RetryPolicies() {}

    public static RetryPolicy simple() {
        return simple(3, Duration.ofSeconds(3));
    }

    public static RetryPolicy simple(int maxAttempts, Duration baseDelay) {
        Verify.state(maxAttempts >= 0, "maxAttempts must be >= 0");
        Verify.nonNull(baseDelay, "baseDelay");
        return new SimpleRetryPolicy(maxAttempts, baseDelay);
    }

    private static final class SimpleRetryPolicy implements RetryPolicy {

        private final int maxAttempts;
        private final Duration baseDelay;

        private SimpleRetryPolicy(int maxAttempts, Duration baseDelay) {
            this.maxAttempts = maxAttempts;
            this.baseDelay = baseDelay;
        }

        @Override
        public RetryDecision onFailure(CrawlTask task, Throwable error, Instant now) {
            if (task == null || now == null) {
                return RetryDecision.deadLetter("Invalid retry evaluation context");
            }

            int attempt = task.attempt();

            // attempt is "current attempt number"
            // if attempt >= maxAttempts -> DLQ
            if (attempt >= maxAttempts) {
                return RetryDecision.deadLetter("Max attempts reached: " + maxAttempts);
            }

            // Backoff: baseDelay * (attempt + 1)
            Duration delay = baseDelay.multipliedBy((long) attempt + 1L);
            Instant notBefore = now.plus(delay);

            return RetryDecision.retry(notBefore, "Retry after " + delay);
        }
    }
}
