package org.jmouse.security.web.match;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.condition.HttpHeaderMatcher;

public class RequestHeaderRequestMatcher implements RequestMatcher {

    private final Matcher<RequestRoute> matcher;

    public RequestHeaderRequestMatcher(HttpHeader header, Object requiredValue) {
        this.matcher = new HttpHeaderMatcher(header, requiredValue);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return matcher.matches(RequestRoute.ofRequest(request));
    }

    @Override
    public String toString() {
        return "RequestHeaderRequestMatcher[ %s ]".formatted(matcher);
    }

}
