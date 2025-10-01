package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

/**
 * 🔎 AuthenticationInspector
 *
 * Lightweight inspector that classifies the current authentication state of a {@link Authentication}.
 *
 * <p>🌱 Works in CORE layer (no web dependencies).</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>🕶️ Detect if subject is anonymous</li>
 *   <li>🧩 Detect if subject is partially authenticated (weak/ambient)</li>
 *   <li>🛡️ Detect if subject is fully authenticated (strong/interactive)</li>
 *   <li>📊 Provide a coarse-grained {@link AuthenticationLevel} for policy checks</li>
 * </ul>
 */
public interface AuthenticationInspector {

    /**
     * 🕶️ Check if subject has no established identity (guest/anonymous).
     *
     * @param authentication subject authentication
     * @return true if anonymous
     */
    default boolean isAnonymous(Authentication authentication) {
        return level(authentication) == AuthenticationLevel.ANONYMOUS;
    }

    /**
     * 🧩 Check if subject is authenticated only via weak/ambient mechanisms.
     *
     * <p>Examples: remember-me cookies, pre-auth headers without strong proof.</p>
     *
     * @param authentication subject authentication
     * @return true if partially authenticated
     */
    default boolean isPartiallyAuthenticated(Authentication authentication) {
        return level(authentication) == AuthenticationLevel.PARTIAL;
    }

    /**
     * 🛡️ Check if subject passed strong/interactive authentication.
     *
     * <p>Examples: username/password, MFA, strong tokens.</p>
     *
     * @param authentication subject authentication
     * @return true if fully authenticated
     */
    default boolean isFullyAuthenticated(Authentication authentication) {
        return !isPartiallyAuthenticated(authentication) && !isAnonymous(authentication);
    }

    /**
     * 📊 Get coarse-grained classification level of subject.
     *
     * @param authentication subject authentication
     * @return {@link AuthenticationLevel} for policy checks and UI hints
     */
    default AuthenticationLevel level(Authentication authentication) {
        return authentication.level();
    }
}
