package org.jmouse.security.web.csrf;

import org.jmouse.security.Authentication;
import org.jmouse.security.web.session.SessionAuthenticateHandler;
import org.jmouse.web.http.RequestContextKeeper;

/** Rotates the synchronizer token after authentication, before a redirect or response commits. */
public final class CsrfAuthenticationStrategy implements SessionAuthenticateHandler {
    private final CsrfTokenRepository repository;
    private final boolean allowSessionCreation;

    public CsrfAuthenticationStrategy(CsrfTokenRepository repository, boolean allowSessionCreation) {
        this.repository = java.util.Objects.requireNonNull(repository, "repository");
        this.allowSessionCreation = allowSessionCreation;
    }

    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {
        if (repository.requiresSession() && !allowSessionCreation && keeper.request().getSession(false) == null) {
            return;
        }
        repository.clear(keeper.request(), keeper.response());
        CsrfToken token = repository.loadOrCreate(keeper.request(), keeper.response());
        keeper.request().setAttribute(CsrfToken.REQUEST_ATTRIBUTE, token);
        keeper.response().setHeader(token.headerName(), token.value());
        keeper.response().setHeader("Cache-Control", "no-store");
    }
}
