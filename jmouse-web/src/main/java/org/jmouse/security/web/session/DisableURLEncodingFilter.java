package org.jmouse.security.web.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class DisableURLEncodingFilter implements BeanFilter {

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(requestContext.request(), new DisableURLEncodingResponse(requestContext.response()));
    }

    public static class DisableURLEncodingResponse extends HttpServletResponseWrapper {

        public DisableURLEncodingResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public String encodeURL(String url) {
            return url;
        }

        @Override
        public String encodeRedirectURL(String url) {
            return url;
        }

    }

}
