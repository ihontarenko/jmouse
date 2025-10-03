package org.jmouse.security.core.session;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;

class InMemorySessionRegistry implements SessionRegistry {

    private final Map<String, Object>             principals = new ConcurrentHashMap<>();
    private final Map<Object, Set<String>>        sessionIds = new ConcurrentHashMap<>();
    private final Map<String, SessionInformation> metadata   = new ConcurrentHashMap<>();

    @Override
    public void registerSession(String sessionId, Object principal) {
        principals.put(sessionId, principal);
        metadata.put(sessionId, new SessionInformation(sessionId, principal, Instant.now()));
        sessionIds.computeIfAbsent(principal, __ -> newKeySet()).add(sessionId);
    }

    @Override
    public void removeSession(String sessionId) {
        Object principal = principals.remove(sessionId);
        metadata.remove(sessionId);
        if (principal != null) {
            Set<String> sessionIds = this.sessionIds.get(principal);
            if (sessionIds != null) {
                sessionIds.remove(sessionId);
                if (sessionIds.isEmpty()) {
                    this.sessionIds.remove(principal);
                }
            }
        }
    }

    @Override
    public Optional<Object> getPrincipal(String sessionId) {
        return Optional.ofNullable(principals.get(sessionId));
    }

    @Override
    public Set<String> getSessionIds(Object principal) {
        return sessionIds.getOrDefault(principal, Set.of());
    }

    @Override
    public Optional<SessionInformation> getSessionInformation(String sessionId) {
        return Optional.ofNullable(metadata.get(sessionId));
    }

}