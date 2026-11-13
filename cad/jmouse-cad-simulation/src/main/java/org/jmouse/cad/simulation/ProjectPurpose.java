package org.jmouse.cad.simulation;

import java.util.Locale;
import java.util.Optional;

/**
 * What a project is for, as its own text says it — {@code meta { purpose drafts }} —: one design of sheets, or the place its
 * owner keeps what is tried before it is a design of its own. A project that says nothing is a design.
 */
public enum ProjectPurpose {

    DESIGN,
    DRAFTS;

    /** The word a text says it with: {@code drafts}. */
    public String word() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The purpose a text says; a design when it says none. A word naming no purpose is refused, saying which are. */
    public static ProjectPurpose written(Optional<String> word) {
        if (word.isEmpty() || word.get().isBlank()) {
            return DESIGN;
        }
        for (ProjectPurpose purpose : values()) {
            if (purpose.word().equals(word.get().strip())) {
                return purpose;
            }
        }
        throw new IllegalArgumentException("A project is for a design or drafts, not '" + word.get().strip() + "'");
    }
}
