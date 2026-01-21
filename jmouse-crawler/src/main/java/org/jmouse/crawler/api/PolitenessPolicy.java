package org.jmouse.crawler.api;

import java.time.Instant;

/**
 * Policy that determines when a request to a given target becomes eligible
 * for execution, enforcing crawl politeness and rate-limiting rules. 🕊️
 *
 * <p>{@code PolitenessPolicy} is consulted by the scheduler before a
 * {@link ProcessingTask} is executed.</p>
 *
 * <p>The policy expresses its decision in terms of time:
 * it returns the earliest {@link Instant} at which a request may be issued.</p>
 *
 * <p>Returning {@code null} or an instant not after {@code now} indicates
 * that the request is immediately eligible.</p>
 *
 * <p>Typical use cases include:</p>
 * <ul>
 *   <li>per-host minimum delay enforcement</li>
 *   <li>global or per-domain rate limiting</li>
 *   <li>robots.txt–aware crawl pacing</li>
 * </ul>
 */
public interface PolitenessPolicy {

    /**
     * Determine the earliest instant at which a request to the given task
     * becomes eligible for execution.
     *
     * <p>If the returned instant is {@code null} or not after {@code now},
     * the request is considered immediately eligible.</p>
     *
     * <p>If the returned instant is in the future, the scheduler should
     * defer the associated task until that time.</p>
     *
     * @param task the target URL of the request
     * @param now the current instant used as a reference point
     * @return the earliest eligibility instant, or {@code null}
     *         if no politeness delay applies
     */
    Instant eligibleAt(ProcessingTask task, Instant now);
}
