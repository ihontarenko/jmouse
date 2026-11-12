package org.jmouse.security.web.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Objects;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

/** Session logout. Register after CSRF protection; only POST can invalidate authentication. */
public class LogoutAuthenticationFilter implements BeanFilter {

    private final String path;

    public LogoutAuthenticationFilter() {
        this("/logout");
    }

    public LogoutAuthenticationFilter(String path) {
        this.path = Objects.requireNonNull(path, "path");
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Logout path must be absolute");
        }
    }

    @Override
    public void doFilterInternal(RequestContext context, FilterChain chain)
        throws IOException, ServletException {
        String requestPath = context
            .request()
            .getRequestURI()
            .substring(context.request().getContextPath().length());
        if (!path.equals(requestPath) || !"POST".equals(context.request().getMethod())) {
            chain.doFilter(context.request(), context.response());
            return;
        }
        SecurityContextHolder.clearContext();
        var session = context.request().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        context.response().setHeader("Cache-Control", "no-store");
        context.response().setStatus(204);
    }
}
