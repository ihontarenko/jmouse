package org.jmouse.crawler.api;

import org.jmouse.crawler.runtime.core.RetryDecision;

import java.time.Instant;

public interface RetryPolicy {
    RetryDecision onFailure(ProcessingTask task, Throwable error, Instant now);
}
