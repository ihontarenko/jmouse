package org.jmouse.files.directory;

import org.jmouse.files.exception.DirectoryException;

import java.util.Locale;

/**
 * 🔧 One kind of thing a folder can be configured to do, and the shape of that configuration.
 *
 * <p>A directory carries at most one configuration per kind, and a kind is contributed by whoever
 * cares about it: {@code upload} says what may enter, and a later {@code retention} or {@code naming}
 * would say something else entirely. The point of naming them rather than adding columns is that the
 * second kind costs a record and a registry entry, and no migration at all.</p>
 *
 * <h3>⚠️ The payload is a document, never a key/value bag</h3>
 *
 * <p>{@code (directory_id, key, value)} of loose strings extends exactly as well and stays untyped
 * forever, with every reader parsing it its own way and none of them agreeing about what an absent key
 * means. A document bound into {@link #payloadType} is typed on both sides — the route that writes it
 * validates it as this record, and the resolver that reads it gets this record or an error, never a
 * map of strings it has to interpret.</p>
 *
 * <p><strong>This is the line somebody will later "simplify" into a bag.</strong> It is not a
 * simplification; it is the same table with the typing removed.</p>
 *
 * @param name        what this kind is called, lower-cased — it is written into a row and into a URL
 * @param payloadType the record a configuration of this kind binds into
 * @param <T>         that record's type
 */
public record DirectoryConfigurationKind<T>(String name, Class<T> payloadType) {

    /** Longest a kind's name may be, matching the column that stores it. */
    public static final int MAXIMUM_NAME_LENGTH = 64;

    /**
     * 🏗️ Refuse a kind that could not be stored or addressed.
     */
    public DirectoryConfigurationKind {
        String candidate = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);

        if (candidate.isEmpty()) {
            throw new DirectoryException("A directory configuration kind needs a name.");
        }

        if (candidate.length() > MAXIMUM_NAME_LENGTH) {
            throw new DirectoryException(
                "A configuration kind's name may be up to %d characters — '%s' is %d."
                    .formatted(MAXIMUM_NAME_LENGTH, candidate, candidate.length()));
        }

        if (payloadType == null) {
            throw new DirectoryException(
                "Configuration kind '%s' needs the type its payload binds into.".formatted(candidate));
        }

        name = candidate;
    }

    /**
     * 🏗️ Name a kind and the record it carries.
     *
     * @param name        what to call it
     * @param payloadType the record a configuration of this kind binds into
     * @param <T>         that record's type
     * @return the kind
     */
    public static <T> DirectoryConfigurationKind<T> of(String name, Class<T> payloadType) {
        return new DirectoryConfigurationKind<>(name, payloadType);
    }

    @Override
    public String toString() {
        return name;
    }
}
