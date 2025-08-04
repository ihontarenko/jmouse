package org.jmouse.mvc;

public interface RoutingInfo {

    MappedHandler handler();

    RouteMatch match();

    Route route();

    record Default(MappedHandler handler, RouteMatch match, Route route) implements RoutingInfo {}

    static RoutingInfo of(MappedHandler handler, RouteMatch match, Route route) {
        return new Default(handler, match, route);
    }

}
