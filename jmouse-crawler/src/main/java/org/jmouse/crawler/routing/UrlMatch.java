package org.jmouse.crawler.routing;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.runtime.CrawlRunContext;

@FunctionalInterface
public interface UrlMatch {
    boolean test(CrawlTask task, CrawlRunContext run);
}
