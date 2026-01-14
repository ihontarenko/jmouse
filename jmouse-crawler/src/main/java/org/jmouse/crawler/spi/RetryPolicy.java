package org.jmouse.crawler.spi;

import org.jmouse.crawler.runtime.CrawlTask;

import java.time.Instant;

public interface RetryPolicy {
    RetryDecision onFailure(CrawlTask task, Throwable error, Instant now);
}
