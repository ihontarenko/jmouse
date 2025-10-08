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
 * Delegates incoming requests to the first matching {@link SecurityFilterChain}.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>🔍 Iterate over all configured {@link SecurityFilterChain}s</li>
 *   <li>✅ Select the first one whose {@link SecurityFilterChain#matches(HttpServletRequest)} returns true</li>
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
    private final List<SecurityFilterChain> chains;

    /**
     * 🏗️ Construct delegator with injected chains.
     *
     * @param chains aggregated security filter chains
     */
    @BeanConstructor
    public SecurityFilterChainDelegator(@AggregatedBeans List<SecurityFilterChain> chains) {
        this.chains = chains;
    }

    /**
     * 🔄 Delegates request to the first matching {@link SecurityFilterChain}.
     *
     * @param request  incoming servlet request
     * @param response servlet response
     * @param chain    outer application filter chain
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        for (SecurityFilterChain securityChain : chains) {
            if (securityChain.matches((HttpServletRequest) request)) {
                // Wrap filters into a virtual chain
                Virtual virtual = new Virtual(chain, securityChain.getFilters());

                LOGGER.info(colorize(
                                "🛡️ ${BLUE_BOLD_BRIGHT}SECURITY${RESET} ➡️ CHAIN ➡️ [${GREEN_BOLD_BRIGHT}{}${RESET}] ➡️ URI: ${RED_BOLD_BRIGHT}{}${RESET}"),
                        virtual, ((HttpServletRequest) request).getRequestURI());

                virtual.doFilter(request, response);
                return; // ✅ stop after first match
            }
        }

        // no match -> fall back to outer chain
        chain.doFilter(request, response);
    }

    /**
     * 🔗 Virtual chain — executes the filters of a matched {@link SecurityFilterChain}
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
