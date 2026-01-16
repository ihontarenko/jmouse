package org.jmouse.crawler.spi;

import org.jmouse.crawler.runtime.ProcessingTask;

import java.time.Instant;

public interface RetryPolicy {
    RetryDecision onFailure(ProcessingTask task, Throwable error, Instant now);
}
