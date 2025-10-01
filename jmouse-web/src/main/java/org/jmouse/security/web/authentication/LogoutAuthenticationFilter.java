package org.jmouse.security.web.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class LogoutAuthenticationFilter implements BeanFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    }

}
