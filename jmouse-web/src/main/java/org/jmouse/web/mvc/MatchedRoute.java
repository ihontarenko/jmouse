package org.jmouse.web.mvc;

import org.jmouse.web.match.Route;
import org.jmouse.web.match.RouteMatch;

public record MatchedRoute<H>(
        boolean matched,
        H handler,
        Route route,
        RouteMatch routeMatch
) {
    public static <H> MatchedRoute<H> notMatched() {
        return new MatchedRoute<>(false, null, null, null);
    }
}
