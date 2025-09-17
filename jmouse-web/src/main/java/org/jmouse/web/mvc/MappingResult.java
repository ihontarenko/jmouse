package org.jmouse.web.mvc;

public interface MappingResult {

    MappingResult EMPTY = new Default(null, null);

    RouteMatch match();

    Route route();

    record Default(RouteMatch match, Route route) implements MappingResult {}

    static MappingResult of(RouteMatch match, Route route) {
        return new Default(match, route);
    }

}
