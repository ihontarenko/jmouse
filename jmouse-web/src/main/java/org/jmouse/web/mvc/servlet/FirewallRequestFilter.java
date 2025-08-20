package org.jmouse.web.mvc.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.security.firewall.Decision;
import org.jmouse.web.security.firewall.EvaluationInput;
import org.jmouse.web.security.firewall.Firewall;

import java.io.IOException;

import static org.jmouse.web.http.HttpHeader.X_FIREWALL_REASON;

@Bean
public class FirewallRequestFilter implements Filter, InitializingBeanSupport<WebBeanContext> {

    private Firewall firewall;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Decision decision = firewall.evaluate(EvaluationInput.from((HttpServletRequest) request));

        if (decision != null && decision.isNotAllowed() && response instanceof HttpServletResponse servletResponse) {
            servletResponse.setHeader(X_FIREWALL_REASON.toString(), decision.reason());
            servletResponse.setStatus(decision.httpStatus().getCode());
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void doInitialize(WebBeanContext context) {
        firewall = context.getBean(Firewall.class);
    }

}
