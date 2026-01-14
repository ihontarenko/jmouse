package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;

public interface CrawlRouteResolver {
    CrawlRoute resolve(CrawlTask task, CrawlRunContext run);
}