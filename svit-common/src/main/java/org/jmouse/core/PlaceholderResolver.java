package org.jmouse.core;

/**
 * Resolves placeholders in strings (e.g., {@code ${app.name}}`).
 */
@FunctionalInterface
public interface PlaceholderResolver {

    PlaceholderResolver NOOP = (value) -> value;

    /**
     * Resolves placeholders in the given text.
     */
    String resolvePlaceholder(String placeholder);

}