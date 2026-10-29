package org.jmouse.security.web.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.Set;

public final class CsrfFilter implements BeanFilter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private final CsrfTokenRepository repository;
    private final SessionCreationPolicy policy;

    public CsrfFilter(CsrfTokenRepository repository, SessionCreationPolicy policy) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.policy = Objects.requireNonNull(policy, "policy");
        if (policy.isStateless() && repository.requiresSession()) {
            throw new IllegalStateException("A session CSRF repository cannot be used in stateless mode");
        }
    }

    @Override
    public void doFilterInternal(RequestContext context, FilterChain chain) throws IOException, ServletException {
        boolean safe = SAFE_METHODS.contains(context.request().getMethod());
        if (repository.requiresSession() && !policy.isAllowCreate()
                && context.request().getSession(false) == null) {
            if (safe) {
                chain.doFilter(context.request(), context.response());
            } else {
                context.response().sendError(403, "CSRF token required");
            }
            return;
        }
        CsrfToken token = repository.loadOrCreate(context.request(), context.response());
        context.request().setAttribute(CsrfToken.REQUEST_ATTRIBUTE, token);
        context.response().setHeader(token.headerName(), token.value());
        context.response().setHeader("Cache-Control", "no-store");
        if (!safe && !validToken(context, token)) {
            context.response().sendError(403, "Invalid CSRF token");
            return;
        }
        chain.doFilter(context.request(), context.response());
    }

    private boolean validToken(RequestContext context, CsrfToken expected) {
        var headers = context.request().getHeaders(expected.headerName());
        String supplied = null;
        if (headers != null && headers.hasMoreElements()) {
            supplied = headers.nextElement();
            if (headers.hasMoreElements()) {
                return false;
            }
        }
        if (supplied == null) {
            String[] parameters = context.request().getParameterValues(expected.parameterName());
            if (parameters != null && parameters.length == 1) {
                supplied = parameters[0];
            }
        }
        return supplied != null && MessageDigest.isEqual(
                expected.value().getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
}
