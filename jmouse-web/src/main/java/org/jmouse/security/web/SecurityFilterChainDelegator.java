package org.jmouse.security.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.core.Streamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.jmouse.core.AnsiColors.colorize;

/**
 * 🛡️ SecurityFilterChainDelegator
 *
 * Delegates incoming requests to the first matching {@link MatchableSecurityFilterChain}.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>🔍 Iterate over all configured {@link MatchableSecurityFilterChain}s</li>
 *   <li>✅ Select the first one whose {@link MatchableSecurityFilterChain#matches(HttpServletRequest)} returns true</li>
 *   <li>🔗 Wrap its filters in a {@link Virtual} {@link FilterChain} for sequential execution</li>
 *   <li>📢 Log which chain matched and for which URI</li>
 * </ul>
 *
 * <p>💡 Similar to Spring Security’s {@code FilterChainProxy}, but streamlined for jMouse.</p>
 */
@Bean
public class SecurityFilterChainDelegator implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilterChainDelegator.class);

    /**
     * 🔗 All registered security filter chains (aggregated from beans).
     */
    private final List<MatchableSecurityFilterChain> chains;

    /**
     * 🏗️ Construct delegator with injected chains.
     *
     * @param chains aggregated security filter chains
     */
    @BeanConstructor
    public SecurityFilterChainDelegator(@AggregatedBeans List<MatchableSecurityFilterChain> chains) {
        this.chains = chains;
    }

    /**
     * 🔄 Delegates request to the first matching {@link MatchableSecurityFilterChain}.
     *
     * @param request  incoming servlet request
     * @param response servlet response
     * @param chain    outer application filter chain
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        for (MatchableSecurityFilterChain securityChain : chains) {
            if (securityChain.matches((HttpServletRequest) request)) {
                Virtual virtual = new Virtual(chain, securityChain.getFilters());

                logMatcherRequest((HttpServletRequest) request);

                for (Filter filter : securityChain.getFilters()) {
                    Filter original = unwrap(filter);
                    logFilterName(original);
                }

                virtual.doFilter(request, response);
                return;
            }
        }

        // no match -> fall back to outer chain
        chain.doFilter(request, response);
    }

    private Filter unwrap(Filter filter) {
        Filter original = filter;
        if (filter instanceof OrderedFilter orderedFilter) {
            original = orderedFilter.filter();
        }
        return original;
    }

    private void logMatcherRequest(HttpServletRequest request) {
        LOGGER.info(colorize("\uD83D\uDEE1\uFE0F\uD83D\uDD10 ${BLUE_BOLD_BRIGHT}SECURITY${RESET} ➡️ URI: ${RED_BOLD_BRIGHT}{}${RESET}"),
                    request.getRequestURI());
    }

    private void logFilterName(Filter original) {
        LOGGER.info(colorize("➡️ ${BLUE_BOLD_BRIGHT}FILTER:${RESET} ${YELLOW_BOLD_BRIGHT}{}${RESET}"),
                    original.getClass().getName());
    }

    /**
     * 🔗 Virtual chain — executes the filters of a matched {@link MatchableSecurityFilterChain}
     * before delegating back to the outer application {@link FilterChain}.
     */
    private static final class Virtual implements FilterChain {

        private final List<Filter> filters;
        private final FilterChain  outer;
        private       int          index = 0;

        Virtual(FilterChain outer, List<Filter> filters) {
            this.outer = outer;
            this.filters = filters;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            if (index < filters.size()) {
                Filter next = filters.get(index++);
                next.doFilter(request, response, this);
            } else {
                outer.doFilter(request, response);
            }
        }

        /**
         * 📝 Returns a formatted representation of the virtual chain,
         * listing the filters (resolving {@link OrderedFilter} to its delegate name).
         */
        @Override
        public String toString() {
            return "Virtual[%s]".formatted(
                    Streamable.of(filters).map(f -> {
                        String name = f.getClass().getSimpleName();
                        if (f instanceof OrderedFilter of) {
                            name = of.filter().getClass().getSimpleName();
                        }
                        return name;
                    }).joining(", ")
            );
        }
    }
}
