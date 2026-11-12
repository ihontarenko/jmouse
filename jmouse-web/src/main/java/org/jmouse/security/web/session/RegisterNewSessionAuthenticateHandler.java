package org.jmouse.security.web.session;

import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import org.jmouse.security.Authentication;
import org.jmouse.security.session.SessionRegistry;
import org.jmouse.web.http.RequestContextKeeper;

/** Registers a stable identity, respecting the caller's session creation and concurrency policies. */
public class RegisterNewSessionAuthenticateHandler implements SessionAuthenticateHandler {

    private final SessionRegistry sessionRegistry;
    private final Integer maximumSessions;
    private final boolean allowCreate;

    public RegisterNewSessionAuthenticateHandler(SessionRegistry sessionRegistry) {
        this(sessionRegistry, null, true);
    }

    public RegisterNewSessionAuthenticateHandler(
        SessionRegistry sessionRegistry,
        Integer maximumSessions,
        boolean allowCreate
    ) {
        this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry");
        if (maximumSessions != null && maximumSessions <= 0) {
            throw new IllegalArgumentException("maximumSessions must be positive");
        }
        this.maximumSessions = maximumSessions;
        this.allowCreate = allowCreate;
    }

    /** Servlet invalidation/timeout unbinds this value and removes its registry metadata. */
    private record RegistryBinding(
        SessionRegistry registry,
        String sessionId
    ) implements jakarta.servlet.http.HttpSessionBindingListener {
        @Override
        public void valueUnbound(jakarta.servlet.http.HttpSessionBindingEvent event) {
            registry.removeSession(sessionId);
        }
    }

    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {
        HttpSession session = keeper.request().getSession(allowCreate);
        if (session == null) {
            return;
        }
        String principal = Objects.requireNonNull(authentication.getName(), "principal name");
        session.setAttribute(
            RegisterNewSessionAuthenticateHandler.class.getName(),
            new RegistryBinding(sessionRegistry, session.getId())
        );
        if (maximumSessions == null) {
            sessionRegistry.registerSession(session.getId(), principal);
        } else {
            sessionRegistry.registerSession(session.getId(), principal, maximumSessions);
        }
    }
}
