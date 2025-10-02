package org.jmouse.security.web.session;

/**
 * 🗝️ SessionCreationPolicy
 *
 * Defines how HTTP sessions should be handled in security/web contexts.
 *
 * <p>Policies:</p>
 * <ul>
 *   <li>🔄 {@link #ALWAYS} — always create a session, even if not used</li>
 *   <li>⚖️ {@link #IF_REQUIRED} — create only when needed (e.g. storing authentication)</li>
 *   <li>👀 {@link #IF_PRESENT} — never create, but allow reading an existing session</li>
 *   <li>🚫 {@link #STATELESS} — completely stateless, do not read or write sessions</li>
 * </ul>
 */
public enum SessionCreationPolicy {

    /**
     * 🔄 Always create a session (eager creation).
     */
    ALWAYS,

    /**
     * ⚖️ Create session only when required (lazy creation).
     */
    IF_REQUIRED,

    /**
     * 👀 Never create a session, but read existing if present.
     */
    IF_PRESENT,

    /**
     * 🚫 Stateless mode — do not use sessions at all.
     */
    STATELESS;

    /**
     * @return true if policy is {@link #ALWAYS} (eager creation)
     */
    public boolean isForceEager() {
        return this == ALWAYS;
    }

    /**
     * @return true if policy is {@link #IF_REQUIRED} (create session only when needed)
     */
    public boolean isIfRequired() {
        return this == IF_REQUIRED;
    }

    /**
     * @return true if policy allows creating a session ({@link #ALWAYS} or {@link #IF_REQUIRED})
     */
    public boolean isAllowCreate() {
        return isIfRequired() || isForceEager();
    }

    /**
     * @return true if policy is {@link #IF_PRESENT} (read-only access to existing session)
     */
    public boolean isReadOnly() {
        return this == IF_PRESENT;
    }

    /**
     * @return true if policy is {@link #STATELESS} (no session usage at all)
     */
    public boolean isStateless() {
        return this == STATELESS;
    }
}
