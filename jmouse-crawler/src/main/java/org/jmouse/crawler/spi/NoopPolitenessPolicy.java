package org.jmouse.crawler.spi;

import java.net.URI;
import java.time.Instant;

/**
 * No-op implementation of {@link PolitenessPolicy}. 🚫🕊️
 *
 * <p>{@code NoopPolitenessPolicy} applies no politeness or rate-limiting
 * constraints. All requests are considered immediately eligible.</p>
 *
 * <p>This implementation is useful for:</p>
 * <ul>
 *   <li>testing and benchmarks</li>
 *   <li>controlled environments (single-host crawls)</li>
 *   <li>situations where politeness is enforced externally</li>
 * </ul>
 *
 * <p>Implemented as a singleton enum to ensure zero allocation
 * and safe reuse across the system.</p>
 */
public enum NoopPolitenessPolicy implements PolitenessPolicy {

    /**
     * Singleton instance.
     */
    INSTANCE;

    /**
     * Always return the provided instant, indicating immediate eligibility.
     *
     * @param url the request URL (ignored)
     * @param now current time reference
     * @return {@code now}, unmodified
     */
    @Override
    public Instant eligibleAt(URI url, Instant now) {
        return now;
    }
}
