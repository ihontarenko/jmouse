package org.jmouse.el.grammar;


import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParseException;

/**
 * A text that cannot be read: which text, which line and column, and what is wrong there — the engine's
 * {@link ParseException}, located in the author's text.
 *
 * <p>Reading refuses only what is structurally wrong — a block that never closes, a line its block's grammar does not
 * read. Whether a name a line uses exists is refused where it is resolved.</p>
 */
public class GrammarRefusal extends ParseException {

    private final String document;
    private final int    line;
    private final int    column;
    private final String detail;

    public GrammarRefusal(String document, int line, int column, String detail) {
        super(document + ", line " + line + (column > 0 ? ", column " + column : "") + ": " + detail);
        this.document = document;
        this.line = line;
        this.column = column;
        this.detail = detail;
    }

    /** A refusal at a token of the text a cursor reads. */
    public static GrammarRefusal at(TokenCursor cursor, Token token, String detail) {
        return new GrammarRefusal(cursor.source().getName(), token.lineNumber(), AuthoredText.of(cursor).column(token), detail);
    }

    /** A refusal where the cursor stands. */
    public static GrammarRefusal here(TokenCursor cursor, String detail) {
        return at(cursor, cursor.current(), detail);
    }

    public String document() {
        return document;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    /** What is wrong, without where. */
    public String detail() {
        return detail;
    }
}
