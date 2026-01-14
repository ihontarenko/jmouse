package org.jmouse.crawler.routing;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.runtime.CrawlRunContext;

public interface CrawlRoute {

    String id();

    CrawlPipeline pipeline();

    boolean matches(CrawlTask task, CrawlRunContext run);

}