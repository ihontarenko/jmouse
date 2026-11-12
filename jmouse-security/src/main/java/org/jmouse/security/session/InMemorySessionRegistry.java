package org.jmouse.security.session;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Single-node registry. All index changes are atomic; returned ID sets are immutable snapshots. */
public class InMemorySessionRegistry implements SessionRegistry {

    private final Map<String, SessionInformation> sessions = new HashMap<>();
    private final Map<Object, Set<String>> sessionIds = new HashMap<>();

    @Override
    public synchronized void registerSession(String sessionId, Object principal) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(principal, "principal");
        removeSession(sessionId);
        sessions.put(sessionId, new SessionInformation(sessionId, principal, Instant.now()));
        sessionIds.computeIfAbsent(principal, key -> new HashSet<>()).add(sessionId);
    }

    @Override
    public synchronized void removeSession(String sessionId) {
        SessionInformation removed = sessions.remove(sessionId);
        if (removed != null) {
            Set<String> identifiers = sessionIds.get(removed.principal());
            identifiers.remove(sessionId);
            if (identifiers.isEmpty()) {
                sessionIds.remove(removed.principal());
            }
        }
    }

    @Override
    public synchronized Optional<Object> getPrincipal(String sessionId) {
        return getSessionInformation(sessionId).map(SessionInformation::principal);
    }

    @Override
    public synchronized Set<String> getSessionIds(Object principal) {
        return Set.copyOf(sessionIds.getOrDefault(principal, Set.of()));
    }

    @Override
    public synchronized Optional<SessionInformation> getSessionInformation(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
}
