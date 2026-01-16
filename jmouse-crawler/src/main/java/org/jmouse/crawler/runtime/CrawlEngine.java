package org.jmouse.crawler.runtime;

import java.time.Instant;

public interface CrawlEngine {
    TaskDisposition execute(CrawlTask task);
    void apply(CrawlTask task, TaskDisposition disposition, Instant now);
}
