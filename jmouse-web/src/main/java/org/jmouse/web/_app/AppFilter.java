package org.jmouse.web._app;

import jakarta.servlet.*;
import org.jmouse.beans.annotation.Provide;

import java.io.IOException;

@Provide
public class AppFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                                     ServletException {
        System.out.println(request);
    }
}
