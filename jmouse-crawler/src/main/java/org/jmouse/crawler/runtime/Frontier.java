package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;

public interface Frontier {
    void offer(CrawlTask task);
    CrawlTask poll();
    int size();
}
