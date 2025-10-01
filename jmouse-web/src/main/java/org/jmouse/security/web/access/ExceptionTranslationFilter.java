package org.jmouse.security.web.access;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.web.AccessDeniedHandler;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.web.servlet.filter.BeanFilter;

public class ExceptionTranslationFilter implements BeanFilter {

    private final AuthenticationEntryPoint entryPoint;
    private final AccessDeniedHandler      deniedHandler;

    public ExceptionTranslationFilter(AuthenticationEntryPoint ep, AccessDeniedHandler dh) {
        this.entryPoint = ep; this.deniedHandler = dh;
    }

    @Override public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
            throws java.io.IOException, ServletException {

        HttpServletRequest  req = (HttpServletRequest) rq;
        HttpServletResponse res = (HttpServletResponse) rs;

        try {
            chain.doFilter(req, res);
        } catch (org.jmouse.security.authorization.AccessDeniedException ade) {
            deniedHandler.handle(req, res, ade);
        } catch (org.jmouse.security.authentication.AuthenticationException ae) {
            entryPoint.initiate(req, res, ae);
        }
    }

}
