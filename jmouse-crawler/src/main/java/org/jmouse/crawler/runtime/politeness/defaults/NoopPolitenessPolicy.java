package org.jmouse.crawler.runtime.politeness.defaults;

import org.jmouse.crawler.api.PolitenessPolicy;
import org.jmouse.crawler.api.ProcessingTask;

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
     * @param task the request task (ignored)
     * @param now current time reference
     * @return {@code now}, unmodified
     */
    @Override
    public Instant eligibleAt(ProcessingTask task, Instant now) {
        return now;
    }
}
