package org.jmouse.java.parser.model;

import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaKeywordDefinition;
import java.util.Optional;

/**
 * A declaration modifier. Annotations are kept separately, as {@link Annotation}.
 */
public enum Modifier {

    PUBLIC(JavaKeyword.PUBLIC),
    PROTECTED(JavaKeyword.PROTECTED),
    PRIVATE(JavaKeyword.PRIVATE),
    STATIC(JavaKeyword.STATIC),
    FINAL(JavaKeyword.FINAL),
    ABSTRACT(JavaKeyword.ABSTRACT),
    NATIVE(JavaKeyword.NATIVE),
    SYNCHRONIZED(JavaKeyword.SYNCHRONIZED),
    TRANSIENT(JavaKeyword.TRANSIENT),
    VOLATILE(JavaKeyword.VOLATILE),
    STRICTFP(JavaKeyword.STRICTFP),
    /** The interface {@code default} method modifier. */
    DEFAULT(JavaKeyword.DEFAULT),
    SEALED(JavaKeyword.SEALED),
    NON_SEALED(JavaKeyword.NON_SEALED);

    private final JavaKeyword keyword;

    Modifier(JavaKeyword keyword) {
        this.keyword = keyword;
    }

    /**
     * Returns the modifier as written in source.
     *
     * @return the spelling, for example {@code non-sealed}
     */
    public String spelling() {
        return keyword.spelling();
    }

    public static Optional<Modifier> fromKeyword(JavaKeywordDefinition keyword) {
        for (Modifier modifier : values()) {
            if (modifier.spelling().equals(keyword.spelling())) {
                return Optional.of(modifier);
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the modifier spelled by a reserved keyword.
     *
     * @param keyword a keyword's text
     * @return the modifier, or {@code null} when the keyword is not a modifier
     */
    public static Modifier ofKeyword(String keyword) {
        for (Modifier modifier : values()) {
            if (modifier != SEALED && modifier != NON_SEALED && modifier.keyword.spelling().equals(keyword)) {
                return modifier;
            }
        }
        return null;
    }
}
