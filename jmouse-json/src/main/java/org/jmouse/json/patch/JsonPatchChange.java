package org.jmouse.json.patch;

import java.util.Locale;
import java.util.Optional;

/**
 * 🩹 One operation of a JSON Patch (RFC 6902) as far as what it changes goes: what it does, where, and where from — not the value it
 * writes. What a reader of the patched document needs to know to read again only what changed.
 *
 * @param operation what it does
 * @param path      where, as a JSON Pointer (RFC 6901): {@code /tracks/3/points}
 * @param from      where a move or a copy takes its value from; empty for every other operation
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record JsonPatchChange(Operation operation, String path, Optional<String> from) {

    /** What an operation of a JSON Patch does. */
    public enum Operation {
        ADD, REMOVE, REPLACE, MOVE, COPY, TEST;

        /** The operation an {@code op} of a patch names: {@code add}, {@code replace} … */
        public static Operation of(String word) {
            return valueOf(word.toUpperCase(Locale.ROOT));
        }
    }

    /** An operation with no {@code from}. */
    public static JsonPatchChange at(Operation operation, String path) {
        return new JsonPatchChange(operation, path, Optional.empty());
    }
}
