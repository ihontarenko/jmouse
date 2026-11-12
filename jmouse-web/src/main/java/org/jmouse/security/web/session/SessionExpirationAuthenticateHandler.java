package org.jmouse.security.web.session;

import java.util.Comparator;
import java.util.List;
import org.jmouse.security.Authentication;
import org.jmouse.security.session.SessionRegistry;
import org.jmouse.web.http.RequestContextKeeper;

/**
 * ⏳ SessionExpirationAuthenticateHandler
 *
 * Expires the oldest sessions for a principal to enforce a maximum concurrent
 * sessions policy.
 *
 * <p>🧠 Semantics:</p>
 * <ul>
 *   <li>Keeps at most <code>maxSessions - 1</code> existing sessions before the current login,
 *       leaving room for the new session being established.</li>
 *   <li>Orders sessions by {@link SessionRegistry.SessionInformation#lastAccess()} and
 *       expires the oldest first.</li>
 * </ul>
 *
 * <p>💡 Example (maxSessions = 2): if 3 sessions exist, this handler expires the 2 oldest so that
 * only 1 remains; after the current login registers, total = 2.</p>
 */
public class SessionExpirationAuthenticateHandler implements SessionAuthenticateHandler {

    /**
     * 🔢 Maximum concurrent sessions allowed per principal.
     */
    private final int maxSessions;
    /**
     * 📇 Registry of sessions keyed by principal.
     */
    private final SessionRegistry sessionRegistry;

    /**
     * 🏗️ Create a new handler.
     *
     * @param maxSessions     maximum allowed concurrent sessions per principal
     * @param sessionRegistry session registry to query/expire sessions
     */
    public SessionExpirationAuthenticateHandler(int maxSessions, SessionRegistry sessionRegistry) {
        if (maxSessions <= 0) {
            throw new IllegalArgumentException("maximumSessions must be positive");
        }
        this.maxSessions = maxSessions;
        this.sessionRegistry = java.util.Objects.requireNonNull(sessionRegistry, "sessionRegistry");
    }

    /**
     * ✅ On successful authentication: enforce the concurrency limit.
     *
     * <p>If the principal already has more than {@code maxSessions} sessions,
     * expire the oldest so that only {@code maxSessions - 1} remain prior to
     * registering the current one.</p>
     *
     * @param authentication authenticated subject (may be {@code null})
     * @param keeper         request/response holder (unused here, but available)
     */
    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {
        Object principal = authentication != null ? authentication.getName() : null;
        if (principal != null) {
            List<SessionRegistry.SessionInformation> sessions = new java.util.ArrayList<>(
                sessionRegistry
                    .getSessions(principal)
                    .stream()
                    .filter(information -> !information.isExpired())
                    .toList()
            );
            if (sessions.size() >= maxSessions) {
                int toExpire = sessions.size() - (maxSessions - 1);
                sessions.sort(Comparator.comparing(SessionRegistry.SessionInformation::lastAccess));
                List<SessionRegistry.SessionInformation> expiredSessions = sessions.subList(0, toExpire);
                expiredSessions.forEach(SessionRegistry.SessionInformation::expireSession);
            }
        }
    }
}
