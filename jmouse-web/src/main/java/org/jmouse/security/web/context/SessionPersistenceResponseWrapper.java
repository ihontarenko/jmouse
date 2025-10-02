package org.jmouse.security.web.context;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.core.SecurityContext;
import org.jmouse.web.http.request.RequestContextKeeper;

import java.io.IOException;

public class SessionPersistenceResponseWrapper extends HttpServletResponseWrapper {

    private final SecurityContextRepository repository;
    private final RequestContextKeeper keeper;
    private final boolean              allowRewrite;

    public SessionPersistenceResponseWrapper(
            HttpServletResponse response, SecurityContextRepository repository, RequestContextKeeper keeper,
            boolean allowRewrite) {
        super(response);
        this.repository = repository;
        this.keeper = keeper;
        this.allowRewrite = allowRewrite;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return allowRewrite ? super.encodeRedirectURL(url) : url;
    }

    @Override
    public String encodeURL(String url) {
        return allowRewrite ? super.encodeURL(url) : url;
    }

    @Override
    public void sendError(int statusCode) throws IOException {
        persistSecurityContext();
        super.sendError(statusCode);
    }

    @Override
    public void sendError(int statusCode, String message) throws IOException {
        persistSecurityContext();
        super.sendError(statusCode, message);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        persistSecurityContext();
        super.sendRedirect(location);
    }

    private void persistSecurityContext() {
        if (SecurityContextHolder.getContext() instanceof SecurityContext securityContext) {
            repository.save(securityContext, keeper);
        }
    }
}
