package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.web.request.RequestRoute;
import org.jmouse.web.request.http.HttpHeader;

public class RequestHeaderCondition implements MappingMatcher {

    private final HttpHeader header;
    private final Object     requiredValue;

    public RequestHeaderCondition(HttpHeader header, Object requiredValue) {
        this.header = header;
        this.requiredValue = requiredValue;
    }

    @Override
    public boolean matches(RequestRoute requestRoute) {
        Object requestValue = requestRoute.headers().getHeader(header);

        if (requestValue != null) {
            return requestValue.equals(requiredValue);
        }

        return false;
    }

    @Override
    public int compareWith(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof RequestHeaderCondition condition))
            return 0;
        return header.compareTo(condition.header);
    }
}
