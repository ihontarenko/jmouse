package org.jmouse.security.web.match;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.web.RequestMatcher;

public class DispatcherTypeRequestMatcher implements RequestMatcher {

    private final DispatcherType dispatcherType;

    public DispatcherTypeRequestMatcher(DispatcherType dispatcherType) {
        this.dispatcherType = dispatcherType;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return request.getDispatcherType() == dispatcherType;
    }

}
