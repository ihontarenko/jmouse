package org.jmouse.web.match.routing.condition;

import jakarta.servlet.DispatcherType;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

public class DispatcherTypeMatcher implements MappingMatcher {

    private final DispatcherType dispatcherType;

    public DispatcherTypeMatcher(DispatcherType dispatcherType) {
        this.dispatcherType = dispatcherType;
    }

    @Override
    public boolean matches(RequestRoute requestRoute) {
        return match(requestRoute) != null;
    }

    @Override
    public <R> R match(RequestRoute requestRoute) {
        R result = null;

        if (requestRoute.request().getDispatcherType() == dispatcherType) {
            result = (R) dispatcherType;
        }

        return result;
    }

    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        return 0;
    }

}
