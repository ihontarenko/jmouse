package org.jmouse.core;

import java.util.Properties;

/**
 * A standard implementation of {@link PlaceholderReplacer} that replaces placeholders
 * in a given string using a specified prefix, suffix, and separator.
 * <p>
 * Supports default values within placeholders (e.g., {@code ${key:default}}) and
 * detects circular references to prevent infinite recursion.
 */
public class StandardPlaceholderReplacer implements PlaceholderReplacer {

    private final String                          prefix;
    private final String                          suffix;
    private final String                          separator;
    private final CyclicReferenceDetector<String> detector;

    private final static IllegalStateException ILLEGAL_STATE_EXCEPTION
            = new IllegalStateException("Circular placeholder reference");

    /**
     * Creates a {@code StandardPlaceholderReplacer} with custom delimiters.
     *
     * @param prefix    the prefix for placeholders (e.g., "${")
     * @param suffix    the suffix for placeholders (e.g., "}")
     * @param separator the separator for default values (e.g., ":")
     */
    public StandardPlaceholderReplacer(String prefix, String suffix, String separator) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.separator = separator;
        this.detector = new DefaultCyclicReferenceDetector<>();
    }

    /**
     * Creates a {@code StandardPlaceholderReplacer} with default delimiters:
     * <ul>
     *     <li>Prefix: ${</li>
     *     <li>Suffix: }</li>
     *     <li>Separator: :</li>
     * </ul>
     */
    public StandardPlaceholderReplacer() {
        this(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, PLACEHOLDER_SEPARATOR);
    }

    /**
     * Replaces placeholders in the given string using a {@link Properties} instance as the source of values.
     *
     * @param value      the string containing placeholders
     * @param properties the properties to resolve placeholder values
     * @return the string with resolved placeholders
     */
    public String replace(String value, Properties properties) {
        return replace(value, properties::getProperty);
    }

    /**
     * Replaces placeholders in the given string using a {@link PlaceholderResolver}.
     *
     * @param value    the string containing placeholders
     * @param resolver the resolver used to retrieve placeholder values
     * @return the string with resolved placeholders
     */
    @Override
    public String replace(String value, PlaceholderResolver resolver) {
        return parse(value, resolver);
    }

    /**
     * Parses and replaces placeholders recursively.
     *
     * @param value    the input string
     * @param resolver the resolver for placeholder values
     * @return the resolved string
     */
    private String parse(String value, PlaceholderResolver resolver) {
        int start = value.indexOf(prefix);

        if (start == -1) {
            return value;
        }

        StringBuilder builder = new StringBuilder(value);

        while (start != -1) {
            int end = builder.indexOf(suffix, start);

            if (end != -1) {
                String placeholder = builder.substring(start + prefix.length(), end);
                String resolved;

                if (placeholder.contains(separator)) {
                    // Handle default value syntax: ${key:default}
                    int    separatorIndex = placeholder.indexOf(separator);
                    String defaultValue   = placeholder.substring(separatorIndex + separator.length());

                    resolved = resolver.resolvePlaceholder(placeholder.substring(0, separatorIndex), defaultValue);
                } else {
                    // Handle simple value syntax: ${app.name}
                    resolved    = resolver.resolvePlaceholder(placeholder, null);
                }

                // Detect circular references
                detector.detect(() -> placeholder, () -> ILLEGAL_STATE_EXCEPTION);

                // Recursively resolve nested placeholders
                if (resolved != null) {
                    resolved = parse(resolved, resolver);
                    builder.replace(start, end + suffix.length(), resolved);
                    start = builder.indexOf(prefix, start + resolved.length());
                } else {
                    start = builder.indexOf(prefix, end + suffix.length());
                }

                detector.remove(() -> placeholder);
            } else {
                start = -1;
            }
        }

        return builder.toString();
    }

    @Override
    public String suffix() {
        return suffix;
    }

    @Override
    public String prefix() {
        return prefix;
    }

}
