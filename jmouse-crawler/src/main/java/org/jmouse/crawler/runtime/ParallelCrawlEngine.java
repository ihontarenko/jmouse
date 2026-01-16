package org.jmouse.crawler.runtime;

import java.time.Instant;

public interface ParallelCrawlEngine extends CrawlEngine {
    TaskDisposition execute(CrawlTask task, Instant now);
    void apply(CrawlTask task, TaskDisposition disposition, Instant now);
}
