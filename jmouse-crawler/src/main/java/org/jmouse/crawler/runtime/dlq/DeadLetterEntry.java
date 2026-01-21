package org.jmouse.crawler.runtime.dlq;

import org.jmouse.crawler.api.ProcessingTask;

/**
 * Immutable entry stored in a {@link DeadLetterQueue}. ☠️
 *
 * <p>A {@code DeadLetterEntry} represents a {@link ProcessingTask} that has
 * permanently failed and has been removed from the normal crawling lifecycle.</p>
 *
 * <p>The entry pairs the failed task with a {@link DeadLetterItem} describing
 * the failure cause, context, and any diagnostic information.</p>
 *
 * <p>Instances of this record are intended to be:</p>
 * <ul>
 *   <li>immutable and thread-safe</li>
 *   <li>cheap to store and transfer</li>
 *   <li>suitable for logging, inspection, or persistence</li>
 * </ul>
 *
 * @param task the task that failed permanently
 * @param item metadata describing the failure reason and context
 */
public record DeadLetterEntry(ProcessingTask task, DeadLetterItem item) {}
