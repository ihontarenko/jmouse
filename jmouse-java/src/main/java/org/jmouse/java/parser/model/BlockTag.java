package org.jmouse.java.parser.model;

import java.util.List;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A block JavaDoc tag such as {@code @param name text}.
 *
 * @param kind           the recognised tag, or {@link Kind#OTHER}
 * @param name           the tag name as written, without {@code @} — {@code exception} stays {@code exception}
 * @param argument       the tag's subject: the parameter name for {@code @param} ({@code <T>} for a type
 *                       parameter), the type for {@code @throws}, the reference for {@code @see}; otherwise {@code null}
 * @param text           the description after the argument, trimmed, with line prefixes removed; may be empty
 * @param referenceState {@link ReferenceState#UNRESOLVED} for {@code @see} references and {@code @throws} types
 * @param inlineTags     inline tags inside this block tag, in order
 * @param span           from the {@code @} to the end of the tag's last non-blank character
 */
public record BlockTag(Kind kind, String name, String argument, String text, ReferenceState referenceState,
                       List<InlineTag> inlineTags, SourceSpan span) {

    /**
     * Copies the list.
     */
    public BlockTag {
        inlineTags = List.copyOf(inlineTags);
    }

    /**
     * The block tags this parser recognises.
     */
    public enum Kind {

        /** {@code @param} */
        PARAM,

        /** {@code @return} */
        RETURN,

        /** {@code @throws} and its synonym {@code @exception} */
        THROWS,

        /** {@code @see} */
        SEE,

        /** {@code @since} */
        SINCE,

        /** {@code @deprecated} */
        DEPRECATED,

        /** {@code @author} */
        AUTHOR,

        /** Any other block tag, kept with its name and text. */
        OTHER;

        /**
         * Maps a tag name to its kind.
         *
         * @param name the name without {@code @}
         * @return the kind, {@link #OTHER} when not recognised
         */
        public static Kind ofName(String name) {
            return switch (name) {
                case "param" -> PARAM;
                case "return" -> RETURN;
                case "throws", "exception" -> THROWS;
                case "see" -> SEE;
                case "since" -> SINCE;
                case "deprecated" -> DEPRECATED;
                case "author" -> AUTHOR;
                default -> OTHER;
            };
        }
    }
}
