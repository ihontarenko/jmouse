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

/**
 * 🛡️ Servlet {@link Filter} that integrates the jMouse {@link Firewall}.
 *
 * <p>Intercepts incoming requests, evaluates them against firewall rules,
 * and either blocks the request or allows it to proceed.</p>
 *
 * <p>💡 Automatically registered as a bean via {@link Bean}.</p>
 */
@Bean
public class FirewallRequestFilter implements Filter, InitializingBeanSupport<WebBeanContext> {

    /** 🔒 Active firewall instance retrieved from the context. */
    private Firewall firewall;

    /**
     * 🔎 Evaluate the incoming request with the firewall.
     *
     * <ul>
     *   <li>If <b>blocked</b> → respond with configured HTTP status and
     *       {@code X-Firewall-Reason} header</li>
     *   <li>If <b>allowed</b> → delegate to the next filter in the chain</li>
     * </ul>
     *
     * @param request  incoming servlet request
     * @param response servlet response
     * @param chain    filter chain for delegation
     */
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

    /**
     * ⚙️ Initialize this filter by retrieving the {@link Firewall} bean
     * from the active {@link WebBeanContext}.
     *
     * @param context current web bean context
     */
    @Override
    public void doInitialize(WebBeanContext context) {
        firewall = context.getBean(Firewall.class);
    }
}
