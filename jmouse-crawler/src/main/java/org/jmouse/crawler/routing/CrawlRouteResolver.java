package org.jmouse.crawler.routing;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.runtime.CrawlRunContext;

public interface CrawlRouteResolver {
    CrawlRoute resolve(CrawlTask task, CrawlRunContext run);
}