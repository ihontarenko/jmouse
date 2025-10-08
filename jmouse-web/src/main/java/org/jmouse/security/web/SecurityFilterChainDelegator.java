package org.jmouse.security.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.core.AnsiColors;
import org.jmouse.core.Streamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Bean
public class SecurityFilterChainDelegator implements Filter {

    private final static Logger LOGGER = LoggerFactory.getLogger(SecurityFilterChainDelegator.class);

    private final List<SecurityFilterChain> chains;

    @BeanConstructor
    public SecurityFilterChainDelegator(@AggregatedBeans List<SecurityFilterChain> chains) {
        this.chains = chains;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        for (SecurityFilterChain securityChain : chains) {
            if (securityChain.matches((HttpServletRequest) request)) {
                Virtual virtual = new Virtual(chain, securityChain.getFilters());
                LOGGER.info(AnsiColors.colorize(
                        "🛡️ ${BLUE_BOLD_BRIGHT}SECURITY${RESET} ➡️ FilterChain ➡️ [${GREEN_BOLD_BRIGHT}{}${RESET}] ➡️ URI: ${RED_BOLD_BRIGHT}{}${RESET}"),
                            virtual, ((HttpServletRequest) request).getRequestURI());
                virtual.doFilter(request, response);
            }
        }
    }

    private static final class Virtual implements FilterChain {

        private final List<Filter>        filters;
        private final FilterChain         outer;
        private       int                 index = 0;

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
