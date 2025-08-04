package org.jmouse.mvc;

public interface MappingResult {

    RouteMatch match();

    Route route();

    record Default(RouteMatch match, Route route) implements MappingResult {}

    static MappingResult of(RouteMatch match, Route route) {
        return new Default(match, route);
    }

}
