package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.routing.RequestRoute;
import org.jmouse.mvc.routing.RouteCondition;
import org.jmouse.web.request.http.HttpMethod;

import java.util.HashSet;
import java.util.Set;

public class HttpMethodCondition implements RouteCondition {

    private final Set<HttpMethod> methods;

    public HttpMethodCondition(HttpMethod... methods) {
        this.methods = Set.of(methods);
    }

    @Override
    public boolean matches(RequestRoute requestRoute) {
        return methods.contains(requestRoute.method());
    }

    @Override
    public RouteCondition combine(RouteCondition other) {
        if (other instanceof HttpMethodCondition condition) {
            Set<HttpMethod> combined = new HashSet<>(this.methods);
            combined.addAll(condition.methods);
            return new HttpMethodCondition(combined.toArray(HttpMethod[]::new));
        }
        return this;
    }

    @Override
    public int compareTo(RouteCondition other, RequestRoute requestRoute) {
        if (!(other instanceof HttpMethodCondition condition))
            return 0;
        return Integer.compare(this.methods.size(), condition.methods.size());
    }

}
