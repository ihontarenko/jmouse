package org.jmouse.web.servlet.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.RequestContext;

import java.io.IOException;

/**
 * 🫘 Integration point between Servlet {@link Filter} and jMouse bean lifecycle.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>🔗 Extends {@link Filter} with bean initialization via {@link InitializingBeanSupport}</li>
 *   <li>🌐 Works with {@link WebBeanContext} for dependency injection</li>
 *   <li>📦 Wraps HTTP requests into a {@link RequestContext}</li>
 *   <li>⚡ Delegates to {@link #doFilterInternal(RequestContext, FilterChain)} for type-safe handling</li>
 * </ul>
 */
public interface BeanFilter extends Filter, InitializingBeanSupport<WebBeanContext> {

    /**
     * 🚦 Entry point from Servlet container.
     *
     * <p>Automatically detects {@link HttpServletRequest}/{@link HttpServletResponse},
     * wraps them into {@link RequestContext}, and calls {@link #doFilterInternal}.</p>
     *
     * @param request  servlet request (HTTP or generic)
     * @param response servlet response (HTTP or generic)
     * @param chain    filter chain to continue processing
     * @throws IOException on I/O error
     * @throws ServletException on servlet error
     */
    @Override
    default void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            doFilterInternal(new RequestContext(httpRequest, httpResponse), chain);
            return;
        }
        doFilter(request, response, chain);
    }

    /**
     * ⚡ Implement filter logic with type-safe {@link RequestContext}.
     *
     * @param requestContext wrapper around HTTP request/response
     * @param chain          filter chain to continue processing
     */
    void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException;
}
