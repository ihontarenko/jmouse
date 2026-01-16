package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.CrawlRunContext;
import org.jmouse.crawler.runtime.CrawlTask;

public interface CrawlRouteResolver {
    CrawlRoute resolve(CrawlTask task, CrawlRunContext run);
}