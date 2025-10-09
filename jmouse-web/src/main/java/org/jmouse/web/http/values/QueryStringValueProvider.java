package org.jmouse.web.http.values;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

public class QueryStringValueProvider implements RequestValueProvider {

    @Override
    public String getTargetName() {
        return "QUERY_STRING";
    }

    @Override
    public Set<String> getRequestValues(HttpServletRequest request) {
        String queryString = request.getQueryString();

        if (queryString == null) {
            queryString = "";
        }

        return Set.of(queryString);
    }

}
