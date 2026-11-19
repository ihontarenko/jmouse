package org.jmouse.java.parser.lexer;

import org.jmouse.java.parser.source.SourceSpan;

/**
 * A comment, kept apart from the tokens so it can never be mistaken for code.
 *
 * @param kind the comment form
 * @param text the comment exactly as written in the original, delimiters included
 * @param span where it is
 */
public record JavaComment(Kind kind, String text, SourceSpan span) {

    /**
     * The three comment forms.
     */
    public enum Kind {

        /** {@code // …} to the end of the line, terminator excluded. */
        LINE,

        /** {@code /* … *}{@code /}. */
        BLOCK,

        /** {@code /** … *}{@code /} — a documentation comment. {@code /**}{@code /} is an empty BLOCK. */
        DOCUMENTATION
    }
}
