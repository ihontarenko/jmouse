package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.routing.MappingCondition;
import org.jmouse.mvc.routing.RequestRoute;
import org.jmouse.web.request.http.HttpHeader;

public class RequestHeaderCondition implements MappingCondition {

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
    public MappingCondition combine(MappingCondition other) {
        return this;
    }

    @Override
    public int compareTo(MappingCondition other) {
        if (!(other instanceof RequestHeaderCondition condition))
            return 0;
        return header.compareTo(condition.header);
    }
}
