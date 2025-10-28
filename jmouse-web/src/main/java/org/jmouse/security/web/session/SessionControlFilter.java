package org.jmouse.security.web.session;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jmouse.security.session.SessionRegistry;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.RequestContextKeeper;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public final class SessionControlFilter implements BeanFilter {

    private final SessionRegistry       registry;
    private final SessionInvalidHandler invalidHandler;

    public SessionControlFilter(SessionRegistry registry, SessionInvalidHandler invalidHandler) {
        this.registry = registry;
        this.invalidHandler = invalidHandler;
    }

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest   request  = requestContext.request();
        HttpServletResponse  response = requestContext.response();
        RequestContextKeeper keeper   = RequestContextKeeper.ofRequestContext(requestContext);
        HttpSession          session  = request.getSession(false);

        if (session != null) {
            if (registry.isExpired(session.getId())) {
                try {
                    session.invalidate();
                } catch (IllegalStateException ignored) {}
                invalidHandler.onInvalidSession(keeper);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
