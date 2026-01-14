package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;

public interface CrawlEngine {
    void submit(CrawlTask task);
    void runOnce();
    void runUntilDrained();
}
