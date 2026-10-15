package org.jmouse.git;

import java.util.regex.Pattern;

/**
 * A full commit identifier in lowercase hexadecimal: 40 digits for SHA-1, 64 for SHA-256.
 *
 * @param value the identifier
 */
public record CommitId(String value) {

    private static final Pattern IDENTIFIER = Pattern.compile("[0-9a-f]{40}|[0-9a-f]{64}");

    public CommitId {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("A commit identifier is 40 or 64 lowercase hexadecimal digits.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
