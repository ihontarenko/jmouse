package org.jmouse.crawler.route;

public interface ProcessingRouteRegistry extends ProcessingRouteResolver {
    ProcessingRoute byId(String routeId);
}
