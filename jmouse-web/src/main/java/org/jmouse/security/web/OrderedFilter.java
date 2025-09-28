package org.jmouse.security.web;

import jakarta.servlet.*;
import org.jmouse.core.Ordered;

import java.io.IOException;

public record OrderedFilter(Filter filter, int order) implements Filter, Ordered {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        filter.doFilter(request, response, chain);
    }
}