package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.matcher.Matcher;

final public class RouteMatcher implements Matcher<Route> {

    private final HttpServletRequest request;

    public RouteMatcher(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public boolean matches(Route route) {
        return false;
    }

    public static RouteMatcher ofRequest(HttpServletRequest request) {
        return new RouteMatcher(request);
    }

}
