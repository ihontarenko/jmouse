package org.jmouse.parallax.store.computation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** How this process takes part in computing: how many jobs at once, how often it looks, how long a claim holds. */
@ConfigurationProperties("jmouse.parallax.computation")
public record ComputationProperties(int workerThreads, Duration pollInterval, Duration lease) {

    public ComputationProperties {
        if (workerThreads < 0) {
            workerThreads = 0;
        }
        if (pollInterval == null || pollInterval.isNegative() || pollInterval.isZero()) {
            pollInterval = Duration.ofSeconds(1);
        }
        if (lease == null || lease.compareTo(Duration.ofSeconds(10)) < 0) {
            lease = Duration.ofMinutes(2);
        }
    }
}
