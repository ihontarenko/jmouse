package org.jmouse.mvc;

public record RouteMappedHandler(Object handler, RouteMatch routeMatch, Route route) implements MappedHandler {
}
