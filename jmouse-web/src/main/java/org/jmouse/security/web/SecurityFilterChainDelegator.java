package org.jmouse.security.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.security.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Owns chain filter lifecycle and isolates authentication to a matched HTTP request. */
@Bean
public class SecurityFilterChainDelegator implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilterChainDelegator.class);
    private final List<MatchableSecurityFilterChain> chains;
    private final List<Filter> lifecycleFilters;
    private final List<Filter> initializedFilters = new ArrayList<>();
    private boolean initialized;

    @BeanConstructor
    public SecurityFilterChainDelegator(@AggregatedBeans List<MatchableSecurityFilterChain> chains) {
        this.chains = List.copyOf(chains);
        Set<Filter> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Filter> filters = new ArrayList<>();
        for (MatchableSecurityFilterChain chain : this.chains) {
            for (Filter filter : chain.getFilters()) {
                Filter original = filter instanceof OrderedFilter ordered ? ordered.filter() : filter;
                if (unique.add(original)) {
                    filters.add(original);
                }
            }
        }
        lifecycleFilters = List.copyOf(filters);
    }

    @Override
    public synchronized void init(FilterConfig configuration) throws ServletException {
        if (initialized) {
            return;
        }
        int initializedCount = 0;
        try {
            for (Filter filter : lifecycleFilters) {
                filter.init(configuration);
                initializedCount++;
                initializedFilters.add(filter);
            }
        } catch (ServletException | RuntimeException | Error failure) {
            for (int index = initializedCount - 1; index >= 0; index--) {
                try {
                    lifecycleFilters.get(index).destroy();
                } catch (RuntimeException | Error cleanupFailure) {
                    if (failure != cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
            }
            initializedFilters.clear();
            throw failure;
        }
        initialized = true;
    }

    @Override
    public synchronized void destroy() {
        Throwable failure = null;
        List<Filter> active = new ArrayList<>(initializedFilters.reversed());
        initializedFilters.clear();
        initialized = false;
        for (Filter filter : active) {
            try {
                filter.destroy();
            } catch (RuntimeException | Error exception) {
                if (failure == null) {
                    failure = exception;
                } else if (failure != exception) {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain outer)
        throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            for (MatchableSecurityFilterChain chain : chains) {
                if (chain.matches(httpRequest)) {
                    LOGGER.debug("Security chain matched {}", httpRequest.getRequestURI());
                    SecurityContextHolder.clearContext();
                    try {
                        new Virtual(outer, chain.getFilters()).doFilter(request, response);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return;
                }
            }
        }
        outer.doFilter(request, response);
    }

    private static final class Virtual implements FilterChain {

        private final FilterChain outer;
        private final List<Filter> filters;
        private int index;

        private Virtual(FilterChain outer, List<Filter> filters) {
            this.outer = outer;
            this.filters = filters;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
            if (index < filters.size()) {
                filters.get(index++).doFilter(request, response, this);
            } else {
                outer.doFilter(request, response);
            }
        }
    }
}
