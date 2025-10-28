package org.jmouse.security.session;

import org.jmouse.core.Streamable;

import java.time.Instant;
import java.util.*;

/**
 * 🧩 <b>SessionRegistry</b> — central component for tracking active user sessions.
 *
 * <p>Responsible for:</p>
 * <ul>
 *   <li>📦 Storing associations between {@code sessionId} and {@code principal}.</li>
 *   <li>⏱️ Tracking last access timestamps.</li>
 *   <li>🚫 Managing session expiration.</li>
 * </ul>
 *
 * <p>Concrete implementations (e.g., in-memory or distributed) define
 * the actual storage and lookup strategy.</p>
 *
 * @see SessionInformation
 */
public interface SessionRegistry {

    /**
     * ⚙️ Create a simple in-memory {@link SessionRegistry}.
     *
     * @return a default in-memory implementation
     */
    static SessionRegistry inMemory() {
        return new InMemorySessionRegistry();
    }

    /**
     * 🔍 Retrieve session information by ID.
     *
     * @param sessionId the unique session identifier
     * @return optional session information, or empty if unknown
     */
    default Optional<SessionInformation> getSessionInformation(String sessionId) {
        return Optional.empty();
    }

    /**
     * ⛔ Expire a session if it exists.
     *
     * @param sessionId the ID of the session to mark as expired
     */
    default void expireSession(String sessionId) {
        getSessionInformation(sessionId).ifPresent(SessionInformation::expireSession);
    }

    /**
     * 🧭 Check whether the session has expired.
     *
     * @param sessionId the session ID
     * @return {@code true} if the session is expired, otherwise {@code false}
     */
    default boolean isExpired(String sessionId) {
        return getSessionInformation(sessionId).map(SessionInformation::isExpired).orElse(false);
    }

    /**
     * 👤 Get the associated principal for a given session.
     *
     * @param sessionId the session ID
     * @return optional principal, empty if session not found
     */
    default Optional<Object> getPrincipal(String sessionId) {
        return getSessionInformation(sessionId).map(SessionInformation::principal);
    }

    /**
     * 🧾 Get all sessions belonging to a principal.
     *
     * @param principal the user or system identity
     * @return list of all active sessions for that principal
     */
    default List<SessionInformation> getSessions(Object principal) {
        List<SessionInformation> sessions = new ArrayList<>();

        Streamable.of(getSessionIds(principal))
                .map(this::getSessionInformation)
                .map(Optional::get)
                .forEach(sessions::add);

        return sessions;
    }

    /**
     * 🏷️ Register a new session.
     *
     * @param sessionId unique ID for the session
     * @param principal authenticated principal bound to the session
     */
    void registerSession(String sessionId, Object principal);

    /**
     * 🗑️ Remove a session completely from the registry.
     *
     * @param sessionId the session ID
     */
    void removeSession(String sessionId);

    /**
     * 🔗 Retrieve all session IDs associated with a given principal.
     *
     * @param principal the user or system identity
     * @return set of active session IDs
     */
    Set<String> getSessionIds(Object principal);

    /**
     * 📄 Immutable metadata holder for a registered session.
     *
     * <p>Tracks session ID, principal, last access timestamp, and expiration flag.</p>
     */
    final class SessionInformation {

        private final String  sessionId;
        private final Object  principal;
        private       Instant lastAccess;
        private       boolean expired;

        /**
         * Construct new session info.
         *
         * @param sessionId  unique session identifier
         * @param principal  associated principal
         * @param lastAccess last access timestamp
         */
        public SessionInformation(String sessionId, Object principal, Instant lastAccess) {
            this.sessionId = sessionId;
            this.principal = principal;
            this.lastAccess = lastAccess;
        }

        /**
         * @return unique session identifier
         */
        public String sessionId() {
            return sessionId;
        }

        /**
         * @return associated principal
         */
        public Object principal() {
            return principal;
        }

        /**
         * @return last access timestamp
         */
        public Instant lastAccess() {
            return lastAccess;
        }

        /**
         * 🔄 Update the last access timestamp.
         *
         * @param lastRequest the latest access instant
         */
        public void lastAccess(Instant lastRequest) {
            this.lastAccess = lastRequest;
        }

        /**
         * @return whether this session is expired
         */
        public boolean isExpired() {
            return expired;
        }

        /**
         * ⚰️ Mark this session as expired.
         */
        public void expireSession() {
            this.expired = true;
        }

        @Override
        public String toString() {
            return "SessionInformation[%s][lastRequest=%s]".formatted(sessionId, lastAccess);
        }
    }
}
