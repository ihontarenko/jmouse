package org.jmouse.web.http.request.values;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

public class RequestPathValueProvider implements RequestValueProvider {

    @Override
    public String getTargetName() {
        return "REQUEST_PATH";
    }

    @Override
    public Set<String> getRequestValues(HttpServletRequest request) {
        return Set.of(request.getRequestURI());
    }

}
