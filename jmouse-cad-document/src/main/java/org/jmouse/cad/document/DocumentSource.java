package org.jmouse.cad.document;

import org.jmouse.el.grammar.AuthoredText;
import org.jmouse.el.grammar.QuotedText;
import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.formula.el.FormulaNotation;
import org.jmouse.units.Units;

import java.util.Arrays;

/**
 * A document's text as the lexer reads it, and as the author wrote it. A datasheet's symbols — {@code Ω}, {@code µ},
 * {@code ±}, {@code −}, a superscript power — are written as the lexer's words before it reads them, outside quotes and
 * comments; every position of the read text knows where it stood in the author's, so what a node keeps as written is
 * cut out of the author's text ({@code 4.7 kΩ}, not {@code 4.7 kOhm}).
 *
 * <p>The cursor carries its source ({@link TokenCursor#source()}), so every parser of a reading reaches this one
 * without being handed it.</p>
 */
public final class DocumentSource extends StringSource implements AuthoredText {

    private static final char   PLUS_MINUS      = '±';
    private static final String PLUS_MINUS_READ = "+-";

    private final String original;
    private final int[]  origins;

    private DocumentSource(String name, String read, String original, int[] origins) {
        super(name, read);
        this.original = original;
        this.origins = origins;
    }

    /** A document's text, brought to what the lexer reads. */
    public static DocumentSource of(String name, String text) {
        StringBuilder read = new StringBuilder(text.length() + 16);
        int[] origins = new int[text.length() * 6 + 1];
        int index = 0;
        boolean afterSuperscript = false;
        while (index < text.length()) {
            int verbatim = verbatimEnd(text, index);
            if (verbatim > index) {
                for (int kept = index; kept < verbatim; kept++) {
                    origins[read.length()] = kept;
                    read.append(text.charAt(kept));
                }
                index = verbatim;
                afterSuperscript = false;
                continue;
            }
            char character = text.charAt(index);
            int digit = FormulaNotation.superscriptDigit(character);
            String written = digit >= 0 ? (afterSuperscript ? "" : "^") + digit
                    : character == PLUS_MINUS ? PLUS_MINUS_READ
                    : FormulaNotation.symbol(character).or(() -> Units.symbol(character)).orElse(String.valueOf(character));
            afterSuperscript = digit >= 0;
            for (int offset = 0; offset < written.length(); offset++) {
                origins[read.length()] = index;
                read.append(written.charAt(offset));
            }
            index++;
        }
        origins[read.length()] = text.length();
        return new DocumentSource(name, read.toString(), text, Arrays.copyOf(origins, read.length() + 1));
    }

    /**
     * Where a run kept as written ends, when one starts here: a text in quotes — {@code """ … """} too — or a comment
     * to the end of its line; {@code start} itself when none does.
     */
    private static int verbatimEnd(String text, int start) {
        char character = text.charAt(start);
        if (text.startsWith("\"\"\"", start)) {
            int end = text.indexOf("\"\"\"", start + 3);
            return end < 0 ? text.length() : end + 3;
        }
        if (character == '"' || character == '\'') {
            int end = text.indexOf(character, start + 1);
            return end < 0 ? text.length() : end + 1;
        }
        if (character == '#') {
            int end = text.indexOf('\n', start);
            return end < 0 ? text.length() : end;
        }
        return start;
    }

    /** The author's text from the start of one token to the end of another, both of the read text. */
    @Override
    public String written(Token first, Token last) {
        int from = Math.max(0, Math.min(first.offset(), origins.length - 1));
        int to = Math.max(from, Math.min(last.offset() + last.value().length(), origins.length - 1));
        return original.substring(origins[from], origins[to]).strip();
    }

    /** The author's text between two tokens — a body between its braces —, dedented. */
    public String between(Token open, Token close) {
        int from = origins[Math.max(0, Math.min(open.offset() + open.value().length(), origins.length - 1))];
        int to = origins[Math.max(0, Math.min(close.offset(), origins.length - 1))];
        return QuotedText.dedented(original.substring(from, Math.max(from, to)));
    }

    /** A text in quotes as it says: its quotes off, and a {@code """ … """} one dedented. */
    public static String text(Token token) {
        return QuotedText.of(token);
    }

    /** The source a cursor reads, when it reads a document. */
    public static DocumentSource of(TokenCursor cursor) {
        return (DocumentSource) cursor.source();
    }

    /** The 1-based column a token stands at, in the author's line. */
    @Override
    public int column(Token token) {
        int origin = origins[Math.max(0, Math.min(token.offset(), origins.length - 1))];
        int lineStart = original.lastIndexOf('\n', origin - 1) + 1;
        return origin - lineStart + 1;
    }
}
