package org.jmouse.crawler.routing;

public interface CrawlRouteRegistry extends CrawlRouteResolver {
    CrawlRoute byId(String routeId);
}
