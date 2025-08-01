package org.jmouse.mvc;

public record RouteMappedHandler(Object handler, Route route) implements MappedHandler {
}
