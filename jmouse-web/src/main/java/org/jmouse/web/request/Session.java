package org.jmouse.web.request;

import java.time.Instant;
import java.util.Map;

/**
 * Abstraction for an HTTP session, providing convenient, type‐safe access to session attributes,
 * metadata (creation time, last accessed time, ID), lifecycle operations (invalidate),
 * the ability to regenerate the session ID, and (optionally) set a custom session ID.
 */
public interface Session {

    /**
     * Return the unique session ID, or {@code null} if no session exists.
     *
     * @return the session ID, or {@code null} if no session is active
     */
    String getId();

    /**
     * Return the time when this session was created, as an {@link Instant},
     * or {@code null} if no session exists.
     *
     * @return creation time, or {@code null} if no session
     */
    Instant getCreationTime();

    /**
     * Return the last time this session was accessed, as an {@link Instant},
     * or {@code null} if no session exists.
     *
     * @return last accessed time, or {@code null} if no session
     */
    Instant getLastAccessedTime();

    /**
     * Determine whether the session is new (i.e., was created in this request),
     * or {@code false} if no session exists.
     *
     * @return {@code true} if this session has just been created; {@code false} otherwise
     */
    boolean isNew();

    /**
     * Invalidate this session, removing all attributes and marking it as no longer valid.
     * If no session exists yet, nothing happens.
     */
    void invalidate();

    /**
     * Return an immutable {@link java.util.Map} view of all session attributes.
     * If no session exists, returns an empty map.
     *
     * @return map of attribute names to values
     */
    Map<String, Object> getAttributeMap();
}

