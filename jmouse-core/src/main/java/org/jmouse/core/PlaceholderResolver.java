package org.jmouse.core;

/**
 * Resolves placeholders within strings.
 * <p>
 * Placeholders follow the syntax: <code>${key}</code>, and may include default values: <code>${key:default}</code>.
 * Implementations provide mechanisms to resolve placeholder values dynamically.
 * </p>
 *
 * @see PlaceholderReplacer
 */
@FunctionalInterface
public interface PlaceholderResolver {

    /** A no-operation resolver that returns the input as-is. */
    PlaceholderResolver NOOP = (value, defaultValue) -> value;

    /**
     * Resolves the given placeholder.
     *
     * @param placeholder the placeholder key to resolve (without prefix or suffix)
     * @return the resolved value, or {@code null} if not found
     */
    String resolvePlaceholder(String placeholder, String defaultValue);
}
