package org.jmouse.web.servlet.filter;

import jakarta.servlet.*;
import org.apache.catalina.connector.RequestFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoggingServletFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingServletFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        LOGGER.info("RQ_ID: {}", ((RequestFacade) request).getRequestURL());

        chain.doFilter(request, response);
    }
}
