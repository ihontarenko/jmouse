package org.jmouse.web.servlet.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.web.request.multipart.MultipartResolver;
import org.jmouse.web.request.multipart.SimpleMultipartResolver;

import java.io.IOException;

@Bean
public class MultipartRequestFilter implements Filter {

    private final MultipartResolver multipartResolver = new SimpleMultipartResolver();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequest wrapped = request;

        if (request instanceof HttpServletRequest httpServletRequest && multipartResolver.isMultipart(httpServletRequest)) {
            wrapped = multipartResolver.resolveMultipart(httpServletRequest);
        }

        chain.doFilter(wrapped, response);
    }

}
