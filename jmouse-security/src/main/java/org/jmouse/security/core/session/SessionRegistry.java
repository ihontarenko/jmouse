package org.jmouse.security.core.session;

import org.jmouse.core.Streamable;

import java.time.Instant;
import java.util.*;

public interface SessionRegistry {

    static SessionRegistry inMemory() {
        return new InMemorySessionRegistry();
    }

    default Optional<SessionInformation> getSessionInformation(String sessionId) {
        return Optional.empty();
    }

    default void expireSession(String sessionId) {
        getSessionInformation(sessionId).ifPresent(SessionInformation::expireSession);
    }

    default Optional<Object> getPrincipal(String sessionId) {
        return getSessionInformation(sessionId).map(SessionInformation::principal);
    }

    default List<SessionInformation> getSessions(Object principal) {
        List<SessionInformation> sessions = new ArrayList<>();

        Streamable.of(getSessionIds(principal))
                .map(this::getSessionInformation)
                .map(Optional::get).forEach(sessions::add);

        return sessions;
    }

    void registerSession(String sessionId, Object principal);

    void removeSession(String sessionId);

    Set<String> getSessionIds(Object principal);

    final class SessionInformation {

        private final String  sessionId;
        private final Object  principal;
        private       Instant lastAccess;
        private       boolean expired;

        public SessionInformation(String sessionId, Object principal, Instant lastAccess) {
            this.sessionId = sessionId;
            this.principal = principal;
            this.lastAccess = lastAccess;
        }

        public String sessionId() {
            return sessionId;
        }

        public Object principal() {
            return principal;
        }

        public Instant lastAccess() {
            return lastAccess;
        }

        public void lastAccess(Instant lastRequest) {
            this.lastAccess = lastRequest;
        }

        public boolean isExpired() {
            return expired;
        }

        public void expireSession() {
            this.expired = true;
        }

        @Override
        public String toString() {
            return "SessionInformation[%s][lastRequest=%s]".formatted(sessionId, lastAccess);
        }
    }

}
