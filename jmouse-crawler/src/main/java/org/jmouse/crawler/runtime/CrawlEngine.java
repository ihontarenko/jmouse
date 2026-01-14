package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;

public interface CrawlEngine {

    boolean tick();

    void submit(CrawlTask task);

}
