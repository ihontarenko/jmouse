package org.jmouse.web._app;

import jakarta.servlet.*;
import org.jmouse.beans.annotation.Provide;

import java.io.IOException;

@Provide
public class AppFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        System.out.println(getClass().getName() +": -> "+ request.getServletContext().getServletContextName());
        chain.doFilter(request, response);
    }

}
