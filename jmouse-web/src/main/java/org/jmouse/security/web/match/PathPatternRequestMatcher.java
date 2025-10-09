package org.jmouse.security.web.match;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.web.MatchResult;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.web.http.RequestAttributesHolder;
import org.jmouse.web.http.RequestPath;
import org.jmouse.web.match.PathPattern;
import org.jmouse.web.match.PathPatternCompiler;
import org.jmouse.web.match.RouteMatch;

public class PathPatternRequestMatcher implements RequestMatcher {

    private final PathPattern compiled;

    public PathPatternRequestMatcher(String pattern) {
        this.compiled = PathPatternCompiler.compile(pattern);
    }

    @Override
    public MatchResult match(HttpServletRequest request) {
        RequestPath requestPath = RequestAttributesHolder.getRequestPath();
        MatchResult result      = MatchResult.no();

        if (matches(request)) {
            RouteMatch match = compiled.match(requestPath.path());
            result = MatchResult.of(true, match.variables());
        }

        return result;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return compiled.matches(RequestAttributesHolder.getRequestPath().path());
    }

    @Override
    public String toString() {
        return "PathPatternRequestMatcher[ %s ]".formatted(compiled);
    }
}
