package org.jmouse.crawler.runtime;

import org.jmouse.crawler.routing.CrawlRoute;

public interface CrawlRouteResolver {
    CrawlRoute resolve(CrawlTask task, CrawlRunContext run);
}