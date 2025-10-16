package org.jmouse.security.web;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.List;

public final class DefaultSecurityFilterChain implements SecurityFilterChain {

    private final MappingMatcher<RequestRoute> matcher;
    private final List<Filter>   filters;

    public DefaultSecurityFilterChain(MappingMatcher<RequestRoute> matcher, List<Filter> filters) {
        this.matcher = matcher;
        this.filters = List.copyOf(filters);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return matcher.matches(RequestRoute.ofRequest(request));
    }

    @Override
    public List<Filter> getFilters() {
        return filters;
    }

}