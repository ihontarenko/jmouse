package org.jmouse.licence;

import java.util.Arrays;

/**
 * A constant of the terms the licence table writes as a word — {@code embedded-only}, {@code share-alike} —, so the table and the
 * code name one thing alike and a new word is a new constant.
 */
public interface TermWord {

    /** The word the table writes it with. */
    String word();

    /** The constant a word names; refuses one the type has no constant for, saying which words it has. */
    static <E extends Enum<E> & TermWord> E of(Class<E> type, String word) {
        return Arrays.stream(type.getEnumConstants()).filter(constant -> constant.word().equals(word)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("'" + word + "' is none of " + Arrays.stream(type.getEnumConstants())
                        .map(TermWord::word).toList() + " (" + type.getSimpleName() + ")"));
    }
}
