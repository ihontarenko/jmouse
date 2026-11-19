package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

/**
 * An inline JavaDoc tag such as {@code {@link Type#method() label}}.
 *
 * <p>Text components are taken from the comment with each line's leading whitespace and asterisks
 * removed; line breaks inside a tag are kept as {@code \n}.
 *
 * @param kind           the recognised tag, or {@link Kind#OTHER}
 * @param name           the tag name as written, without {@code @}
 * @param content        everything after the name up to the closing brace, trimmed
 * @param reference      for {@code link}, {@code linkplain} and {@code value}: the program reference as written; otherwise {@code null}
 * @param label          for {@code link} and {@code linkplain}: the label after the reference, or {@code null}
 * @param referenceState whether this tag points elsewhere; never resolved by this parser
 * @param span           from the opening brace to the closing brace
 */
public record InlineTag(Kind kind, String name, String content, String reference, String label,
                        ReferenceState referenceState, SourceSpan span) {

    /**
     * The inline tags this parser recognises.
     */
    public enum Kind {

        /** {@code {@link}} */
        LINK,

        /** {@code {@linkplain}} */
        LINKPLAIN,

        /** {@code {@code}} */
        CODE,

        /** {@code {@literal}} */
        LITERAL,

        /** {@code {@inheritDoc}} — the text is in a supertype and is not copied here. */
        INHERIT_DOC,

        /** {@code {@value}} */
        VALUE,

        /** Any other inline tag, kept with its name and content. */
        OTHER;

        /**
         * Maps a tag name to its kind.
         *
         * @param name the name without {@code @}
         * @return the kind, {@link #OTHER} when not recognised
         */
        public static Kind ofName(String name) {
            return switch (name) {
                case "link" -> LINK;
                case "linkplain" -> LINKPLAIN;
                case "code" -> CODE;
                case "literal" -> LITERAL;
                case "inheritDoc" -> INHERIT_DOC;
                case "value" -> VALUE;
                default -> OTHER;
            };
        }
    }
}
