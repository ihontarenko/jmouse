package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.web.request.RequestRoute;

public class QueryParameterMatcher implements MappingMatcher {

    private final String parameterName;
    private final Object     requiredValue;

    public QueryParameterMatcher(String parameterName, Object requiredValue) {
        this.parameterName = parameterName;
        this.requiredValue = requiredValue;
    }

    @Override
    public boolean matches(RequestRoute requestRoute) {
        Object requestValue = requestRoute.queryParameters().getFirst(parameterName);

        if (requestValue != null) {
            return requestValue.equals(requiredValue);
        }

        return false;
    }

    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof QueryParameterMatcher condition))
            return 0;
        return parameterName.compareTo(condition.parameterName);
    }

    @Override
    public String toString() {
        return "QueryParameterMatcher: [%s=%s]".formatted(parameterName, requiredValue);
    }
}
