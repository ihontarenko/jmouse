package org.jmouse.security.web.access;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authorization.AccessDeniedException;
import org.jmouse.security.web.AccessDeniedHandler;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class ExceptionTranslationFilter implements BeanFilter {

    private final AuthenticationEntryPoint entryPoint;
    private final AccessDeniedHandler      deniedHandler;

    public ExceptionTranslationFilter(AuthenticationEntryPoint entryPoint, AccessDeniedHandler deniedHandler) {
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
        } catch (AccessDeniedException accessDeniedException) {
            deniedHandler.handle(request, response, accessDeniedException);
        } catch (AuthenticationException authenticationException) {
            entryPoint.initiate(request, response, authenticationException);
        }
    }

}
