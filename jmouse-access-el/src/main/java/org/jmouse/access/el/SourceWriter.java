package org.jmouse.access.el;

import java.util.regex.Pattern;

/**
 * Writes values back out in a form the lexer reads the same way — the mirror of {@link SourceReader}.
 *
 * <p>⚠️ <strong>Rendering a name bare is not free.</strong> A scope instance written
 * {@code @SPACE:'my-space'} comes back from the parser as {@code my-space}, and printing that without
 * its quotes produces {@code @SPACE:my-space} — which the lexer reads as {@code my} followed by a
 * subtraction. The file the control room shows an administrator then no longer says what the file it
 * came from said, which is the one property a round-trip exists to have.</p>
 *
 * <p>So a name is written bare only when the lexer would read it back whole: an identifier
 * ({@code [a-zA-Z_][a-zA-Z0-9_]*}), the {@code *} wildcard, or a {@code ${…}} placeholder that has to
 * stay verbatim because resolving it belongs to a later stage.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SourceWriter {

    /** What the lexer accepts as a bare name; anything else has to be quoted. */
    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /** A whole namespace, or every instance of a scope — written bare, never quoted. */
    private static final String WILDCARD = "*";

    private static final String PLACEHOLDER_OPENING = "${";
    private static final String PLACEHOLDER_CLOSING = "}";

    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '"';

    private SourceWriter() {
    }

    /**
     * Writes a name bare where the lexer reads it back whole, and quoted where it does not.
     *
     * @param value the name, as the parser reported it
     * @return the name as it belongs in a file
     */
    public static String name(String value) {
        if (value == null) {
            return null;
        }

        if (WILDCARD.equals(value) || isPlaceholder(value) || IDENTIFIER.matcher(value).matches()) {
            return value;
        }

        return literal(value);
    }

    /**
     * Writes a value the grammar always quotes — a description, an included path, a policy name.
     *
     * <p>⚠️ A {@code .jmp} string literal has no escape sequence, so the quote character has to be
     * chosen rather than escaped. A value holding both kinds cannot be written at all, and saying so
     * is better than emitting a file that will not parse.</p>
     *
     * @param value the value to quote
     * @return the value as a string literal
     * @throws IllegalArgumentException when the value holds both kinds of quote
     */
    public static String literal(String value) {
        if (value == null) {
            return null;
        }

        boolean holdsSingle = value.indexOf(SINGLE_QUOTE) >= 0;
        boolean holdsDouble = value.indexOf(DOUBLE_QUOTE) >= 0;

        if (holdsSingle && holdsDouble) {
            // ⚠️ The parentheses are the fix, not decoration. `"a %s" + "b".formatted(v)` binds the call
            // to the LAST literal only: every placeholder before it survives as a raw %s and the
            // argument meant for it is dropped, silently, because String.format ignores arguments it
            // was not asked for. See JMF-12 — this was the third of three sites.
            throw new IllegalArgumentException(
                    ("'%s' cannot be written to a policy file: a string literal there holds no escapes, "
                     + "so a value carrying both kinds of quote has no spelling").formatted(value));
        }

        char quote = holdsSingle ? DOUBLE_QUOTE : SINGLE_QUOTE;

        return quote + value + quote;
    }

    /**
     * Whether a value is a {@code ${…}} placeholder, which is written exactly as it was read.
     *
     * @param value the value to inspect
     * @return {@code true} when it is a placeholder
     */
    private static boolean isPlaceholder(String value) {
        return value.startsWith(PLACEHOLDER_OPENING) && value.endsWith(PLACEHOLDER_CLOSING);
    }
}
