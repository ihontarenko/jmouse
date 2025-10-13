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
        return requestRoute.request().getDispatcherType() == dispatcherType;
    }

    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        return 0;
    }

}
