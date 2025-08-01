package org.jmouse.mvc.mapping;

import org.jmouse.mvc.adapter.FunctionalRoute;
import org.jmouse.web.request.http.HttpMethod;

public record RouteRegistration(HttpMethod method, String route, FunctionalRoute functionalRoute) {
}
