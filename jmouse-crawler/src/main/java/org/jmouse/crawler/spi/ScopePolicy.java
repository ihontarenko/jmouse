package org.jmouse.crawler.spi;

import org.jmouse.crawler.runtime.ProcessingTask;

public interface ScopePolicy {

    boolean isAllowed(ProcessingTask task);

    String denyReason(ProcessingTask task);

    default boolean isDisallowed(ProcessingTask task) {
        return !isAllowed(task);
    }

}
