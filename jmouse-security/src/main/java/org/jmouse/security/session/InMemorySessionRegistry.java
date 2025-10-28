package org.jmouse.security.session;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;

/**
 * 💾 <b>InMemorySessionRegistry</b> — simple thread-safe {@link SessionRegistry} implementation.
 *
 * <p>Stores all session–principal mappings directly in memory using concurrent maps.</p>
 *
 * <h3>Characteristics</h3>
 * <ul>
 *   <li>⚡ Backed by {@link ConcurrentHashMap} — suitable for lightweight concurrent use.</li>
 *   <li>🧍 Tracks each {@code principal → Set<sessionId>} and session metadata.</li>
 *   <li>🧩 No persistence — data is lost when the application stops.</li>
 * </ul>
 *
 * <h3>Use cases</h3>
 * <ul>
 *   <li>Ideal for prototypes, local testing, or single-node applications.</li>
 *   <li>For distributed systems, replace with a persistent or clustered registry (e.g. Redis).</li>
 * </ul>
 *
 * @see SessionRegistry
 * @see SessionRegistry.SessionInformation
 */
class InMemorySessionRegistry implements SessionRegistry {

    /**
     * Maps session ID → principal.
     */
    private final Map<String, Object> principals = new ConcurrentHashMap<>();

    /**
     * Maps principal → active session IDs.
     */
    private final Map<Object, Set<String>> sessionIds = new ConcurrentHashMap<>();

    /**
     * Maps session ID → session metadata.
     */
    private final Map<String, SessionInformation> metadata = new ConcurrentHashMap<>();

    /**
     * 🏷️ Register a new session for a given principal.
     *
     * @param sessionId unique session identifier
     * @param principal the authenticated user or system identity
     */
    @Override
    public void registerSession(String sessionId, Object principal) {
        principals.put(sessionId, principal);
        metadata.put(sessionId, new SessionInformation(sessionId, principal, Instant.now()));
        sessionIds.computeIfAbsent(principal, __ -> newKeySet()).add(sessionId);
    }

    /**
     * 🗑️ Remove a session and clean up any principal references.
     *
     * @param sessionId the session ID to remove
     */
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

    /**
     * 👤 Get the principal bound to a session ID.
     *
     * @param sessionId the session identifier
     * @return optional principal, or empty if unknown
     */
    @Override
    public Optional<Object> getPrincipal(String sessionId) {
        return Optional.ofNullable(principals.get(sessionId));
    }

    /**
     * 🔗 Retrieve all session IDs associated with a given principal.
     *
     * @param principal the principal identity
     * @return set of session IDs, or empty set if none
     */
    @Override
    public Set<String> getSessionIds(Object principal) {
        return sessionIds.getOrDefault(principal, Set.of());
    }

    /**
     * 🔍 Retrieve full {@link SessionInformation} metadata for a session ID.
     *
     * @param sessionId the session identifier
     * @return optional session metadata, or empty if not found
     */
    @Override
    public Optional<SessionInformation> getSessionInformation(String sessionId) {
        return Optional.ofNullable(metadata.get(sessionId));
    }
}
