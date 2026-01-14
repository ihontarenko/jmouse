package org.jmouse.crawler.runtime;

public interface CrawlEngine {

    boolean tick();

    void submit(CrawlTask task);

}
