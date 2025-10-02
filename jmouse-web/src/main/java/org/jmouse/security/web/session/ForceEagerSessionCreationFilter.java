package org.jmouse.security.web.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class ForceEagerSessionCreationFilter implements BeanFilter {

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = requestContext.request();
        request.getSession(true);
        chain.doFilter(request, requestContext.response());
    }

}
