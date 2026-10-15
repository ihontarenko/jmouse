package org.jmouse.vault;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 🏷️ The address of a secret: a path of kebab-case segments, {@code telegram/notifications/token}.
 *
 * <p>The grammar is deliberately narrow — lower-case letters, digits and single hyphens, segments joined
 * by {@code /} — because a name is spelled in four places that disagree about everything else: a URL
 * query, a database key, a YAML map key and an environment variable. {@link #environmentVariable} is the
 * reason the grammar has no underscore: it makes that mapping reversible ({@link #fromEnvironmentVariable}).
 */
public record SecretName(String value) implements Comparable<SecretName> {

    /** 📏 Generous for a path, and the width of the key column. */
    public static final int MAXIMUM_LENGTH = 255;

    private static final Pattern SEGMENT = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private static final String SEPARATOR             = "/";
    private static final String ENVIRONMENT_SEPARATOR = "__";

    public SecretName {
        Objects.requireNonNull(value, "secret name");

        if (value.isEmpty() || value.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException(
                    "A secret name must be 1 to %d characters long.".formatted(MAXIMUM_LENGTH));
        }

        for (String segment : value.split(SEPARATOR, -1)) {
            if (!SEGMENT.matcher(segment).matches()) {
                throw new IllegalArgumentException(("'%s' is not a secret name: use kebab-case segments joined by "
                        + "'/', for example 'telegram/notifications/token'.").formatted(value));
            }
        }
    }

    public static SecretName of(String value) {
        return new SecretName(value);
    }

    public static SecretName of(String first, String... more) {
        StringBuilder builder = new StringBuilder(first);

        for (String segment : more) {
            builder.append(SEPARATOR).append(segment);
        }

        return new SecretName(builder.toString());
    }

    public List<String> segments() {
        return Arrays.asList(value.split(SEPARATOR));
    }

    public SecretName child(String segment) {
        return new SecretName(value + SEPARATOR + segment);
    }

    /** 🔗 The last segment — a credential's field name when this names one of its secrets. */
    public String lastSegment() {
        int index = value.lastIndexOf(SEPARATOR);

        return index < 0 ? value : value.substring(index + 1);
    }

    public boolean isWithin(SecretName ancestor) {
        return value.startsWith(ancestor.value + SEPARATOR);
    }

    /**
     * 🌍 The environment variable this name is read from: {@code telegram/notifications/token} under
     * prefix {@code RACCOON_VAULT} is {@code RACCOON_VAULT_TELEGRAM__NOTIFICATIONS__TOKEN} — a segment
     * break is a double underscore, a hyphen a single one.
     */
    public String environmentVariable(String prefix) {
        String body = value.replace(SEPARATOR, ENVIRONMENT_SEPARATOR).replace('-', '_').toUpperCase(Locale.ROOT);

        return prefix + "_" + body;
    }

    /** 🔁 The inverse of {@link #environmentVariable}, or null when the variable is not under the prefix. */
    public static SecretName fromEnvironmentVariable(String prefix, String variable) {
        String head = prefix + "_";

        if (!variable.startsWith(head) || variable.length() == head.length()) {
            return null;
        }

        String body = variable.substring(head.length())
                .replace(ENVIRONMENT_SEPARATOR, SEPARATOR)
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);

        try {
            return new SecretName(body);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public int compareTo(SecretName other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
