package org.jmouse.security.web.access;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authorization.AuthorizationException;
import org.jmouse.security.web.AccessDeniedHandler;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.cache.RequestCache;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class ExceptionTranslationFilter implements BeanFilter {

    private final RequestCache             requestCache;
    private final AuthenticationEntryPoint entryPoint;
    private final AccessDeniedHandler      deniedHandler;

    public ExceptionTranslationFilter(
            RequestCache requestCache, AuthenticationEntryPoint entryPoint, AccessDeniedHandler deniedHandler
    ) {
        this.requestCache = requestCache;
        this.entryPoint = entryPoint;
        this.deniedHandler = deniedHandler;
    }

    /**
     * ⚡ Implement filter logic with type-safe {@link RequestContext}.
     *
     * @param requestContext wrapper around HTTP request/response
     * @param chain          filter chain to continue processing
     */
    @Override
    public void doFilterInternal(
            RequestContext requestContext, FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest  request  = requestContext.request();
        HttpServletResponse response = requestContext.response();

        try {
            chain.doFilter(request, response);
        } catch (AuthorizationException accessDeniedException) {
            deniedHandler.handle(request, response, accessDeniedException);
        } catch (AuthenticationException authenticationException) {
            SecurityContextHolder.clearContext();
            if (requestCache != null) {
                requestCache.saveRequest(request, response);
            }
            entryPoint.initiate(request, response, authenticationException);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
