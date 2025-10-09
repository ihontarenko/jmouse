package org.jmouse.security.web.match;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.condition.HttpMethodMatcher;

import java.util.Set;

public class HttpMethodRequestMatcher implements RequestMatcher {

    private final Matcher<RequestRoute> matcher;

    public HttpMethodRequestMatcher(Set<HttpMethod> methods) {
        this.matcher = new HttpMethodMatcher(methods.toArray(HttpMethod[]::new));
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return matcher.matches(RequestRoute.ofRequest(request));
    }

    @Override
    public String toString() {
        return "RequestMethodRequestMatcher[ %s ]".formatted(matcher);
    }

}
