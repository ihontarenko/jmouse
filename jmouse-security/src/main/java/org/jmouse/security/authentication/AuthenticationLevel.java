package org.jmouse.security.authentication;

/**
 * 🔑 Represents the strength of an established authentication.
 *
 * <ul>
 *   <li>🕶️ {@link #ANONYMOUS} — no identity, ambient or weak context (e.g. guest)</li>
 *   <li>🧩 {@link #PARTIAL} — partially identified, interactive but not fully verified</li>
 *   <li>🛡️ {@link #FULL} — strong identity established, trusted session</li>
 * </ul>
 */
public enum AuthenticationLevel {

    /**
     * 🕶️ No authenticated identity (guest / anonymous user).
     */
    ANONYMOUS,

    /**
     * 🧩 Partially authenticated (weak but interactive, e.g. OTP step pending).
     */
    PARTIAL,

    /**
     * 🛡️ Fully authenticated (strong identity, trusted session).
     */
    FULL
}
