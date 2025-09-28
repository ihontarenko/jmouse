package org.jmouse.security.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.Streamable;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.security.web.match.PathPatternRequestMatcher;
import org.jmouse.security.web.match.RequestHeaderRequestMatcher;
import org.jmouse.security.web.match.HttpMethodRequestMatcher;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;

import java.util.Map;
import java.util.Set;

public interface RequestMatcher extends Matcher<HttpServletRequest> {

    static RequestMatcher pathPattern(String pattern) {
        return new PathPatternRequestMatcher(pattern);
    }

    static RequestMatcher header(String header, Object requiredValue) {
        return header(HttpHeader.ofHeader(header), requiredValue);
    }

    static RequestMatcher header(HttpHeader header, Object requiredValue) {
        return new RequestHeaderRequestMatcher(header, requiredValue);
    }

    static RequestMatcher httpMethod(String... httpMethods) {
        return httpMethod(Streamable.of(httpMethods).map(HttpMethod::ofName).toArray(HttpMethod[]::new));
    }

    static RequestMatcher httpMethod(HttpMethod... httpMethods) {
        return new HttpMethodRequestMatcher(Set.of(httpMethods));
    }

    static void main(String[] args) {
        pathPattern("/**").or(
                pathPattern("*")
                        .and(header("X-Requested-With", "XMLHttpRequest").not().and(
                                httpMethod("GET").and(header("Accept", "*/*"))
                        ))
        );
    }

    default MatchResult match(HttpServletRequest request) {
        MatchResult result = MatchResult.no();

        if (matches(request)) {
            result = MatchResult.of(true, Map.of());
        }

        return result;
    }

}
