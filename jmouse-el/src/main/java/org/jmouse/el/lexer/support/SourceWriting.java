package org.jmouse.el.lexer.support;

/**
 * Writes values back out in a form {@link SourceReading} reads the same way. ✍️
 *
 * <h2>⚠️ Why the writing half belongs beside the reading half</h2>
 *
 * <p>{@link SourceReading#literal} strips a quote pair and does nothing else — there is no escape
 * sequence to undo, because this lexer has none. That single fact decides everything about writing a
 * value back: a quote inside the value cannot be escaped, so the <em>other</em> quote character has to
 * be chosen, and a value holding both kinds has no spelling at all.</p>
 *
 * <p>Two languages on this lexer had worked that out separately — {@code .jmp} first, {@code .jmm}
 * about to. Two implementations of one lexer rule is how a file written by one language's tooling
 * stops being readable by another's, and the rule is not either language's to own: it is the lexer's,
 * and this is where the lexer's helpers live.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SourceWriting {

    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '"';

    private SourceWriting() {
    }

    /**
     * Writes a value as a string literal this lexer reads back whole, in double quotes where it can.
     *
     * @param value the value to quote, or {@code null}
     * @return the value as a string literal, or {@code null} for a {@code null} input
     * @throws IllegalArgumentException when the value holds both kinds of quote
     */
    public static String literal(String value) {
        return literal(value, DOUBLE_QUOTE);
    }

    /**
     * Writes a value as a string literal this lexer reads back whole.
     *
     * <p>⚠️ There is no escape sequence, so the quote character is <strong>chosen</strong> rather than
     * escaped. A value holding both kinds cannot be written at all, and saying so is better than
     * emitting a file that will not parse.</p>
     *
     * <h2>⚠️ The rule is shared; the taste is not</h2>
     *
     * <p>Which quote a language reaches for <em>first</em> is a property of that language's files, not
     * of the lexer — {@code .jmp} writes single quotes and its output is stored as policy revisions an
     * installation reverts to, so changing that spelling is a change to every revision written from
     * then on. A caller with a canonical form says what it is; one without takes double quotes, which
     * is what a file somebody typed almost always holds.</p>
     *
     * @param value     the value to quote, or {@code null}
     * @param preferred the quote to use where the value permits either
     * @return the value as a string literal, or {@code null} for a {@code null} input
     * @throws IllegalArgumentException when the value holds both kinds of quote
     */
    public static String literal(String value, char preferred) {
        if (value == null) {
            return null;
        }

        boolean holdsSingle = value.indexOf(SINGLE_QUOTE) >= 0;
        boolean holdsDouble = value.indexOf(DOUBLE_QUOTE) >= 0;

        if (holdsSingle && holdsDouble) {
            throw new IllegalArgumentException(
                    ("'%s' holds both a single and a double quote, and this lexer has no escape "
                     + "sequence — so there is no way to write it down that reads back the same")
                            .formatted(value));
        }

        if (holdsSingle) {
            return DOUBLE_QUOTE + value + DOUBLE_QUOTE;
        }

        if (holdsDouble) {
            return SINGLE_QUOTE + value + SINGLE_QUOTE;
        }

        return preferred + value + preferred;
    }
}
