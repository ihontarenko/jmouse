package org.jmouse.crawler.runtime;

public interface Frontier {
    void offer(CrawlTask task);
    CrawlTask poll();
    int size();
}
