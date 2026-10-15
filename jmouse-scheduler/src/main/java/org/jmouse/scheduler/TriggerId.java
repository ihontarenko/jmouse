package org.jmouse.scheduler;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A trigger's permanent identifier, chosen by the product — {@code nightly-vectorise}, or a key with
 * a product's own row identifier in it, such as {@code source-42.scan}.
 *
 * @param value lower-case letters, digits, {@code -}, {@code .} and {@code _}; at most 200 characters
 */
public record TriggerId(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9][a-z0-9._-]{0,199}");

    public TriggerId {
        Objects.requireNonNull(value, "value");

        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("A trigger identifier is lower-case letters, digits, '.', '_' and '-',"
                    + " at most 200 characters: '" + value + "'.");
        }
    }

    /**
     * @param value the identifier
     * @return the identifier as a value
     */
    public static TriggerId of(String value) {
        return new TriggerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
