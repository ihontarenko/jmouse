package org.jmouse.crawler.runtime;

import java.time.Instant;

public interface ParallelCrawlEngine extends CrawlEngine {

    int moveReadyRetries(int max);

    CrawlTask poll();

    TaskDisposition execute(CrawlTask task, Instant now);

    void apply(CrawlTask task, TaskDisposition disposition, Instant now);

    int frontierSize();

    int retrySize();

    Instant now();

}
