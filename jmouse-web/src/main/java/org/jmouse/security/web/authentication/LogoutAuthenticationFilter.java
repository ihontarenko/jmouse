package org.jmouse.security.web.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class LogoutAuthenticationFilter implements BeanFilter {


    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain) throws IOException, ServletException {

    }
}
