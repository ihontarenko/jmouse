package org.jmouse.crawler.spi;

import org.jmouse.crawler.runtime.CrawlTask;

public interface ScopePolicy {

    boolean isAllowed(CrawlTask task);

    String denyReason(CrawlTask task);

    default boolean isDisallowed(CrawlTask task) {
        return !isAllowed(task);
    }

}
