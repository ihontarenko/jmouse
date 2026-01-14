package org.jmouse.crawler.runtime;

import java.time.Instant;
import java.util.List;

public interface RetryBuffer {
    void schedule(CrawlTask task, Instant notBefore, String reason, Throwable error);
    List<CrawlTask> drainReady(Instant now, int max);
    int size();
}