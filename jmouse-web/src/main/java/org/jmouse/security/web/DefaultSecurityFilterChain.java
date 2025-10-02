package org.jmouse.security.web;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public final class DefaultSecurityFilterChain implements SecurityFilterChain {

    private final RequestMatcher requestMatcher;
    private final List<Filter>   filters;

    public DefaultSecurityFilterChain(RequestMatcher requestMatcher, List<Filter> filters) {
        this.requestMatcher = requestMatcher;
        this.filters = List.copyOf(filters);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return requestMatcher.matches(request);
    }

    @Override
    public List<Filter> getFilters() {
        return filters;
    }

}