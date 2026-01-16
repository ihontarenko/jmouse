package org.jmouse.crawler.routing;

public interface ProcessingRouteRegistry extends ProcessingRouteResolver {
    ProcessingRoute byId(String routeId);
}
