package org.jmouse.java.parser.model;

/**
 * A declaration modifier. Annotations are kept separately, as {@link Annotation}.
 */
public enum Modifier {

    PUBLIC("public"),
    PROTECTED("protected"),
    PRIVATE("private"),
    STATIC("static"),
    FINAL("final"),
    ABSTRACT("abstract"),
    NATIVE("native"),
    SYNCHRONIZED("synchronized"),
    TRANSIENT("transient"),
    VOLATILE("volatile"),
    STRICTFP("strictfp"),
    /** The interface {@code default} method modifier. */
    DEFAULT("default"),
    SEALED("sealed"),
    NON_SEALED("non-sealed");

    private final String spelling;

    Modifier(String spelling) {
        this.spelling = spelling;
    }

    /**
     * Returns the modifier as written in source.
     *
     * @return the spelling, for example {@code non-sealed}
     */
    public String spelling() {
        return spelling;
    }

    /**
     * Finds the modifier spelled by a reserved keyword.
     *
     * @param keyword a keyword's text
     * @return the modifier, or {@code null} when the keyword is not a modifier
     */
    public static Modifier ofKeyword(String keyword) {
        for (Modifier modifier : values()) {
            if (modifier != SEALED && modifier != NON_SEALED && modifier.spelling.equals(keyword)) {
                return modifier;
            }
        }
        return null;
    }
}
