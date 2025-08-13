package org.jmouse.web.servlet.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.web.request.WebHttpRequest;

import java.io.IOException;

@Bean
public class RequestWrapperFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequest wrapped = request;

        if (request instanceof HttpServletRequest httpServletRequest) {
            wrapped = new WebHttpRequest(httpServletRequest);
        }

        chain.doFilter(wrapped, response);
    }

}
