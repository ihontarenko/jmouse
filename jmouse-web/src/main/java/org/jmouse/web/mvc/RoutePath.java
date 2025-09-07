package org.jmouse.web.mvc;

public interface RoutePath {

    /**
     * Original (raw) pattern string, e.g. "/users/{id:int}" or "/assets/**"
     */
    String raw();

    /**
     * Fast check if path matches
     */
    boolean matches(String path);

    /**
     * Match and extract variables (returns empty RouteMatch if no match)
     */
    RouteMatch match(String path);

    Kind kind();

    enum Kind {ANT, TEMPLATE,}

}
