package org.jmouse.crawler.api;

public interface ScopePolicy {

    boolean isAllowed(ProcessingTask task);

    String denyReason(ProcessingTask task);

    default boolean isDisallowed(ProcessingTask task) {
        return !isAllowed(task);
    }

}
