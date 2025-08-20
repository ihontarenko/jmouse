package org.jmouse.web.http.request.values;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

public class QueryStringValueProvider implements RequestValueProvider {

    @Override
    public String getTargetName() {
        return "QUERY_STRING";
    }

    @Override
    public Set<String> getRequestValues(HttpServletRequest request) {
        return Set.of(request.getQueryString());
    }

}
