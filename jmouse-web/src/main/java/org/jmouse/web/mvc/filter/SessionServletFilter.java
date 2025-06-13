package org.jmouse.web.mvc.filter;

import jakarta.servlet.*;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.web.request.RequestAttributes;
import org.jmouse.web.request.SessionAttributesHolder;

import java.io.IOException;

@Provide
public class SessionServletFilter implements Filter {

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
            throws IOException, ServletException {
        if (rq instanceof HttpServletRequest request) {
            SessionAttributesHolder.setRequestAttributes(
                    RequestAttributes.of(BeanScope.SESSION, request)
            );
        }

        chain.doFilter(rq, rs);
    }
}
