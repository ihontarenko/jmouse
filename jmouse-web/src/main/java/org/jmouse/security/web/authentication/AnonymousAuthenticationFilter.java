package org.jmouse.security.web.authentication;

import jakarta.servlet.*;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AnonymousAuthentication;
import org.jmouse.security.web.SecurityFilter;
import org.jmouse.web.http.RequestContext;

import java.io.IOException;

public class AnonymousAuthenticationFilter implements SecurityFilter {

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain) throws IOException, ServletException {
        var securityContext = SecurityContextHolder.getContext();

        if (securityContext.getAuthentication() == null) {
            securityContext.setAuthentication(new AnonymousAuthentication(AnonymousAuthentication.ANONYMOUS));
        }

        chain.doFilter(requestContext.request(), requestContext.response());
    }

}
