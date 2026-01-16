package org.jmouse.crawler.runtime;

import java.time.Instant;

public interface ProcessingEngine {
    TaskDisposition execute(ProcessingTask task);
    void apply(ProcessingTask task, TaskDisposition disposition, Instant now);
}
