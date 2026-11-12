package org.jmouse.web.servlet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

/**
 * ⏳ PerRequestLimiterFilter
 * Servlet filter that limits the number of executions per HTTP request.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>🔢 Allows up to {@code times} executions per request</li>
 *   <li>🔁 Delegates to {@link #doFilterInternal(RequestContext, FilterChain)} within the limit</li>
 *   <li>🚦 Falls back to the filter chain once the quota is exhausted</li>
 * </ul>
 */
public abstract class PerRequestLimiterFilter implements BeanFilter {

    private final int times;

    /**
     * 🏗️ Create filter with max executions per request.
     *
     * @param times number of allowed invocations for a single request
     */
    protected PerRequestLimiterFilter(int times) {
        if (times <= 0) {
            throw new IllegalArgumentException("Execution limit must be positive");
        }
        this.times = times;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        if (acquireCounter((HttpServletRequest) request)) {
            doFilterInternal(
                new RequestContext((HttpServletRequest) request, (HttpServletResponse) response),
                chain
            );
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * 📊 Increment and check per-request execution counter.
     *
     * @param request current HTTP request
     * @return {@code true} if filter can still run, {@code false} otherwise
     */
    private boolean acquireCounter(HttpServletRequest request) {
        int counter = 0;

        if (request.getAttribute(getFilterAttributeName()) instanceof Integer integer) {
            counter = integer;
        }

        if (counter < times) {
            counter++;
            request.setAttribute(getFilterAttributeName(), counter);
            return true;
        }

        return false;
    }

    /**
     * 🏷️ Attribute name used for storing execution counter.
     *
     * @return filter-specific attribute name
     */
    public String getFilterAttributeName() {
        return getFilterName() + ".EXECUTION";
    }

    /**
     * 🏷️ Resolve unique filter name (default: class name).
     *
     * @return filter name identifier
     */
    private String getFilterName() {
        return getClass().getName();
    }
}
