package org.jmouse.security.web.access;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authorization.AuthorizationException;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.security.web.AuthorizationFailureHandler;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.cache.RequestCache;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class ExceptionTranslationFilter implements BeanFilter {

    private final RequestCache                requestCache;
    private final AuthenticationEntryPoint    entryPoint;
    private final AuthorizationFailureHandler failureHandler;

    public ExceptionTranslationFilter(RequestCache requestCache, AuthenticationEntryPoint entryPoint, AuthorizationFailureHandler failureHandler) {
        this.requestCache = requestCache;
        this.entryPoint = entryPoint;
        this.failureHandler = failureHandler;
    }

    /**
     * ⚡ Executes the filter chain and translates security exceptions.
     *
     * <p>
     * Delegates request further down the chain and intercepts:
     * </p>
     * <ul>
     *     <li>{@link AuthenticationException} → triggers authentication flow</li>
     *     <li>{@link AuthorizationException} → triggers access denied handling</li>
     * </ul>
     *
     * @param requestContext wrapper around HTTP request/response
     * @param chain          filter chain to continue processing
     */
    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request  = requestContext.request();
        HttpServletResponse response = requestContext.response();

        try {
            chain.doFilter(request, response);
        } catch (AuthenticationException exception) {
            handleAuthenticationFailure(request, response, exception);
        } catch (AuthorizationException exception) {
            handleAuthorizationFailure(request, response, exception);
        }
    }

    /**
     * 🔐 Handles authentication failures.
     *
     * <p>
     * Clears current {@link org.jmouse.security.SecurityContext}, optionally saves the request,
     * and delegates to {@link AuthenticationEntryPoint}.
     * </p>
     *
     * @param request   current HTTP request
     * @param response  current HTTP response
     * @param exception authentication failure cause
     */
    protected void handleAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        SecurityContextHolder.clearContext();

        if (requestCache != null) {
            requestCache.saveRequest(request, response);
        }

        entryPoint.initiate(request, response, exception);
    }

    /**
     * 🚫 Handles authorization failures.
     *
     * <p>
     * Delegates to {@link AuthorizationFailureHandler}, typically resulting
     * in HTTP 403 or custom access denied response.
     * </p>
     *
     * @param request   current HTTP request
     * @param response  current HTTP response
     * @param exception authorization failure cause
     */
    protected void handleAuthorizationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthorizationException exception
    ) throws IOException {
        failureHandler.onFailure(request, response, exception);
    }

}
