package org.jmouse.git.hosting;

import java.util.Locale;
import java.util.Optional;

/** Who may read a repository, in the vocabulary both hostings share. */
public enum Visibility {

    /** Anyone, signed in or not. */
    PUBLIC,

    /** Every signed-in member of the instance or enterprise, and nobody else. */
    INTERNAL,

    /** Only those granted access. */
    PRIVATE;

    /** Whether reading the repository needs credentials. */
    public boolean requiresCredentials() {
        return this != PUBLIC;
    }

    /** The visibility a hosting names, if it is one of these. */
    static Optional<Visibility> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "public" -> Optional.of(PUBLIC);
            case "internal" -> Optional.of(INTERNAL);
            case "private" -> Optional.of(PRIVATE);
            default -> Optional.empty();
        };
    }
}
