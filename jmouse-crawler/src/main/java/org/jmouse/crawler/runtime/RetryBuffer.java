package org.jmouse.crawler.runtime;

import java.time.Instant;
import java.util.List;

public interface RetryBuffer {

    void schedule(ProcessingTask task, Instant notBefore, String reason, Throwable error);

    List<ProcessingTask> drainReady(Instant now, int max);

    int size();

    Instant peekNotBefore();

}