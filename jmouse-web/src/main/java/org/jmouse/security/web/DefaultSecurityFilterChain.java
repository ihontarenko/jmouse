package org.jmouse.security.web;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public final class DefaultSecurityFilterChain {

    private final RequestMatcher requestMatcher;
    private final List<Filter>   filters;

    public DefaultSecurityFilterChain(RequestMatcher requestMatcher, List<Filter> filters) {
        this.requestMatcher = requestMatcher;
        this.filters = List.copyOf(filters);
    }

    public boolean matches(HttpServletRequest request) {
        return requestMatcher.matches(request);
    }

    public List<Filter> getFilters() {
        return filters;
    }

}