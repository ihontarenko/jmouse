package org.jmouse.core.mapping.errors;

/**
 * Defines how the mapping engine should handle a mapping error. 🎯
 *
 * <p>{@code ErrorAction} is typically used by error policies to decide whether an exception
 * should abort mapping or be downgraded into a non-fatal failure.</p>
 */
public enum ErrorAction {

    /**
     * Rethrow the error and abort mapping.
     */
    THROW,

    /**
     * Log the error and continue mapping.
     *
     * <p>The sourceRoot mapping call returns {@code null} (or a policy-defined fallback).</p>
     */
    WARNING,

    /**
     * Ignore the error silently and continue mapping.
     *
     * <p>The sourceRoot mapping call returns {@code null} (or a policy-defined fallback).</p>
     */
    SILENT
}
