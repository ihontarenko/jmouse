package org.jmouse.query.el;

import java.util.regex.Pattern;

/**
 * Writes values back out in a form the lexer reads the same way — the other half of a round trip.
 *
 * <p>⚠️ <strong>Rendering a name bare is not free.</strong> A document is stored as text and read back
 * into a builder, so what {@code toSource()} emits has to parse to the node it came from. A projection
 * alias containing a space, printed bare, produces a file the parser then splits in two — and the
 * document an editor shows no longer says what the document it came from said, which is the one
 * property a round trip exists to have.</p>
 *
 * <p>⚠️ <strong>An identifier here is not ASCII-only, and that is deliberate.</strong> Every document
 * this language was designed against is Cyrillic — {@code view "Мої косарки"}, {@code entry[назва]} —
 * so a bare-name test written as {@code [a-zA-Z_]} would quote every real name and produce files that
 * look escaped from end to end. {@code \p{L}} is the whole point.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SourceWriter {

    /** What the lexer accepts as a bare name; anything else has to be quoted. */
    private static final Pattern IDENTIFIER = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*");

    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '"';

    private SourceWriter() {
    }

    /**
     * Writes a name bare where the lexer reads it back whole, and quoted where it does not.
     *
     * @param value the name, as the parser reported it
     * @return the name as it belongs in a document
     */
    public static String name(String value) {
        if (value == null) {
            return null;
        }

        if (IDENTIFIER.matcher(value).matches()) {
            return value;
        }

        return literal(value);
    }

    /**
     * Writes a value the grammar always quotes — a view's title.
     *
     * <p>⚠️ A {@code .jmq} string literal has no escape sequence, so the quote character is chosen
     * rather than escaped. A value holding both kinds cannot be written at all, and saying so is better
     * than emitting a document that will not parse.</p>
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
            throw new IllegalArgumentException(
                    ("'%s' cannot be written to a query document: a string literal there holds no "
                     + "escapes, so a value carrying both kinds of quote has no spelling").formatted(value));
        }

        char quote = holdsSingle ? DOUBLE_QUOTE : SINGLE_QUOTE;

        return quote + value + quote;
    }
}
