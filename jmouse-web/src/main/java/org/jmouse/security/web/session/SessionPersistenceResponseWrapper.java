package org.jmouse.security.web.session;

import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.SecurityContext;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.http.RequestContextKeeper;
import org.jmouse.web.servlet.OnCommitResponseWrapper;

import java.io.IOException;

public class SessionPersistenceResponseWrapper extends OnCommitResponseWrapper {

    private final SecurityContextRepository repository;
    private final RequestContextKeeper      keeper;

    public SessionPersistenceResponseWrapper(SecurityContextRepository repository, RequestContextKeeper keeper) {
        super(keeper.response());
        this.repository = repository;
        this.keeper = keeper;
    }

    @Override
    protected void onBeforeCommit() {
        persistSecurityContext();
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
