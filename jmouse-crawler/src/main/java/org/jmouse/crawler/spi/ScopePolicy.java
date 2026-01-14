package org.jmouse.crawler.spi;

import org.jmouse.crawler.core.CrawlTask;

public interface ScopePolicy {
    boolean isAllowed(CrawlTask task);
    String denyReason(CrawlTask task);
}
