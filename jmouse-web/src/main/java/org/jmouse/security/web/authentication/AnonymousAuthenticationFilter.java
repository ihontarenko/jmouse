package org.jmouse.security.web.authentication;

import jakarta.servlet.*;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AnonymousAuthentication;

import java.io.IOException;

public class AnonymousAuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var securityContext = SecurityContextHolder.getContext();

        if (securityContext.getAuthentication() == null) {
            securityContext.setAuthentication(new AnonymousAuthentication());
        }

        chain.doFilter(request, response);
    }

}
