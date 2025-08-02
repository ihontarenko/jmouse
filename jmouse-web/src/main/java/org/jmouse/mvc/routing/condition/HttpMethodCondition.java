package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.web.request.RequestRoute;
import org.jmouse.web.request.http.HttpMethod;

import java.util.Set;

public class HttpMethodCondition implements MappingMatcher {

    private final Set<HttpMethod> methods;

    public HttpMethodCondition(HttpMethod... methods) {
        this.methods = Set.of(methods);
    }

    @Override
    public boolean matches(RequestRoute requestRoute) {
        return methods.contains(requestRoute.method());
    }

    @Override
    public int compareWith(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof HttpMethodCondition condition))
            return 0;

        return Integer.compare(this.methods.size(), condition.methods.size());
    }

    @Override
    public String toString() {
        return "HttpMethodCondition: %s".formatted(methods);
    }
}
