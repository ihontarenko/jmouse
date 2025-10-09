package org.jmouse.web.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter that exposes {@link HttpSession} operations behind the {@link WebSession} API,
 * backed by the current {@link HttpServletRequest}.
 *
 * <p>Provides lazy access/creation of the underlying session and convenience
 * methods for common attributes and timestamps.</p>
 *
 * @see WebSession
 * @see HttpServletRequest
 * @see HttpSession
 */
public class WebHttpSession extends WebHttpRequest implements WebSession {

    /**
     * 🚦 Flag indicating if new sessions may be created when absent.
     */
    private final boolean allowSessionCreation;

    /**
     * 🏗️ Construct a wrapper for the given request (no session creation).
     *
     * @param request underlying HTTP request
     */
    public WebHttpSession(HttpServletRequest request) {
        this(request, false);
    }

    /**
     * 🏗️ Construct a wrapper for the given request with session creation control.
     *
     * @param request              underlying HTTP request
     * @param allowSessionCreation whether creating a new session is allowed
     */
    public WebHttpSession(HttpServletRequest request, boolean allowSessionCreation) {
        super(request);
        this.allowSessionCreation = allowSessionCreation;
    }

    /**
     * Returns the current {@link HttpSession}, creating one if necessary.
     *
     * @return active session (never {@code null})
     * @throws IllegalStateException if the container fails to create a session
     */
    @Override
    public HttpSession getSession() {
        HttpSession session = getCurrentSession();

        if (session == null) {
            session = getRequest().getSession(allowSessionCreation);
        }

        if (session == null) {
            throw new IllegalStateException("Unable to create session");
        }

        return session;
    }

    /**
     * Returns the current {@link HttpSession} without creating a new one.
     *
     * @return existing session, or {@code null} if none
     */
    @Override
    public HttpSession getCurrentSession() {
        return getRequest().getSession(false);
    }

    /**
     * @return session id, creating the session if necessary
     * @see #getSession()
     */
    @Override
    public String getId() {
        return getSession().getId();
    }

    /**
     * Returns the creation time of the current session, if present.
     *
     * @return creation time as {@link Instant}, or {@code null} if no session exists
     * @see #getCurrentSession()
     */
    @Override
    public Instant getCreationTime() {
        HttpSession session = getCurrentSession();

        if (session == null) {
            return null;
        }

        return Instant.ofEpochMilli(session.getCreationTime());
    }

    /**
     * Returns the last accessed time of the current session, if present.
     *
     * @return last accessed time as {@link Instant}, or {@code null} if no session exists
     * @see #getCurrentSession()
     */
    @Override
    public Instant getLastAccessedTime() {
        HttpSession session = getCurrentSession();

        if (session == null) {
            return null;
        }

        return Instant.ofEpochMilli(session.getLastAccessedTime());
    }

    /**
     * @return {@code true} if the session was created during the current request
     * @throws IllegalStateException if session creation fails
     */
    @Override
    public boolean isNew() {
        return getSession().isNew();
    }

    /**
     * Invalidates the current session if it exists; no-op otherwise.
     */
    @Override
    public void invalidate() {
        HttpSession session;

        if ((session = getCurrentSession()) != null) {
            session.invalidate();
        }
    }

    /**
     * Returns the value of the named session attribute.
     *
     * @param name attribute name
     * @return attribute value, or {@code null} if absent or no session exists
     */
    @Override
    public Object getAttribute(String name) {
        HttpSession session = getSession();

        if (session != null) {
            return session.getAttribute(name);
        }

        return null;
    }

    /**
     * Sets/overwrites the named session attribute, creating the session if necessary.
     *
     * @param name  attribute name
     * @param value attribute value (may be {@code null} depending on container behavior)
     */
    @Override
    public void setAttribute(String name, Object value) {
        getSession().setAttribute(name, value);
    }

    /**
     * Removes the named session attribute, creating the session if necessary.
     *
     * @param name attribute name
     */
    @Override
    public void removeAttribute(String name) {
        getSession().removeAttribute(name);
    }

    /**
     * Returns an immutable snapshot of all session attributes.
     *
     * @return unmodifiable map of attribute names to values; empty if no session exists
     */
    @Override
    public Map<String, Object> getAttributeMap() {
        HttpSession session = getSession();

        if (session == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> attributes = new HashMap<>();
        Enumeration<String> names      = session.getAttributeNames();

        while (names.hasMoreElements()) {
            String key = names.nextElement();
            attributes.put(key, session.getAttribute(key));
        }

        return Collections.unmodifiableMap(attributes);
    }
}
