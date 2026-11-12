package org.jmouse.cad.document;

import java.util.Locale;
import java.util.Optional;

/**
 * Where a catalogue document stands, as its own text says it — {@code meta status draft} —: a draft a curator still
 * works on, published for every reader, or archived — kept, since links to it are out there, but listed no more. A text
 * that says nothing is published, as every document an owner keeps is.
 */
public enum DocumentStatus {

    DRAFT,
    PUBLISHED,
    ARCHIVED;

    /** The word a text says it with: {@code draft}. */
    public String word() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The status a text says; published when it says none. A word naming no status is refused, saying which are. */
    public static DocumentStatus written(Optional<String> word) {
        if (word.isEmpty() || word.get().isBlank()) {
            return PUBLISHED;
        }
        for (DocumentStatus status : values()) {
            if (status.word().equals(word.get().strip())) {
                return status;
            }
        }
        throw new IllegalArgumentException("A document stands as draft, published or archived, not '" + word.get().strip() + "'");
    }
}
