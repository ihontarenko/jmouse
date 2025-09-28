package org.jmouse.security.web;

import java.util.Map;

/**
 * 🎯 Result of evaluating a web request matcher (e.g., path/method/headers).
 *
 * <p>Holds a boolean outcome and an immutable map of extracted variables
 * (such as path-template bindings). The variables map may be empty but is
 * never {@code null}.</p>
 */
public interface MatchResult {

    /**
     * ❌ Non-matching result with no variables.
     *
     * @return a result where {@link #matches()} is {@code false} and {@link #variables()} is empty
     */
    static MatchResult no() {
        return of(false, Map.of());
    }

    /**
     * 🧱 Factory for a match result with explicit variables.
     *
     * <p>The provided map is defensively copied via {@link Map#copyOf(Map)} to ensure immutability.</p>
     *
     * @param ok    whether the matcher succeeded
     * @param variables  variables captured during matching (must not be {@code null})
     * @return an immutable match result
     * @throws NullPointerException if {@code variables} is {@code null}
     */
    static MatchResult of(boolean ok, Map<String, Object> variables) {
        return new Default(ok, Map.copyOf(variables));
    }

    /**
     * ✅ Indicates whether the matcher succeeded.
     *
     * @return {@code true} if matched; {@code false} otherwise
     */
    boolean matches();

    /**
     * 🧩 Immutable variables captured during matching.
     *
     * <p>Never {@code null}. Returns an unmodifiable map; may be empty.</p>
     *
     * @return unmodifiable map of bound variables
     */
    Map<String, Object> variables();

    /**
     * 📦 Compact value-class implementation of {@link MatchResult}.
     */
    record Default(boolean matches, Map<String, Object> variables) implements MatchResult { }
}
