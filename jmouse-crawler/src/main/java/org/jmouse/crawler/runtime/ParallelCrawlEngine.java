package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.core.TaskDisposition;

import java.time.Instant;

public interface ParallelCrawlEngine extends CrawlEngine {

    /**
     * Move ready retries from RetryBuffer back to Frontier.
     * @return number of tasks moved
     */
    int moveReadyRetries(int max);

    /**
     * Poll next task from Frontier (may return null).
     */
    CrawlTask poll();

    /**
     * Execute a single task and return disposition (no side effects like scheduling to retry buffer or DLQ).
     */
    TaskDisposition execute(CrawlTask task, Instant now);

    /**
     * Apply the disposition (retry scheduling / DLQ write / etc).
     */
    void apply(CrawlTask task, TaskDisposition disposition, Instant now);

    int frontierSize();
    int retrySize();

    Instant now();
}
