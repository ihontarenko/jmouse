package org.jmouse.core;

/**
 * Defines a contract for replacing placeholders in strings.
 * <p>
 * Placeholders follow the syntax: <code>${key}</code>, with optional default values: <code>${key:default}</code>.
 * Implementations resolve placeholders using a {@link PlaceholderResolver}.
 *
 * @see PlaceholderResolver
 */
public interface PlaceholderReplacer {

    /** Default prefix for placeholders: <code>${</code> */
    String PLACEHOLDER_PREFIX = "${";

    /** Default suffix for placeholders: <code>}</code> */
    String PLACEHOLDER_SUFFIX = "}";

    /** Default separator for default values inside placeholders: <code>:</code> */
    String PLACEHOLDER_SEPARATOR = ":";

    /**
     * Replaces placeholders in the given string using a {@link PlaceholderResolver}.
     *
     * @param value    the input string containing placeholders
     * @param resolver the resolver used to retrieve placeholder values
     * @return the string with resolved placeholders
     */
    String replace(String value, PlaceholderResolver resolver);
}
