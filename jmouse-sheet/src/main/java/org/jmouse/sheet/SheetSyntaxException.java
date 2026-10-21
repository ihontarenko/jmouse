package org.jmouse.datamap;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;

/**
 * A jMD document that does not read, saying where: the document, the 1-based line and column, and what is wrong.
 *
 * <p>A parser reads a cursor and does not know the file it came from, so the reader stamps the document's name on
 * with {@link #at(String)}.</p>
 */
public class DataMapSyntaxException extends RuntimeException {

    private final String document;
    private final int    line;
    private final int    column;
    private final String detail;

    /** A refusal at the token the cursor stands on. */
    public DataMapSyntaxException(TokenCursor cursor, String detail) {
        this(cursor, cursor.hasNext() ? cursor.lookAt(0) : null, detail);
    }

    /** A refusal at a token already read. */
    public DataMapSyntaxException(TokenCursor cursor, Token token, String detail) {
        this(null, token == null ? 0 : token.lineNumber(), token == null ? 0 : SourceReading.column(cursor, token),
             detail);
    }

    public DataMapSyntaxException(String document, int line, int column, String detail) {
        super((document == null ? "" : document + ", ") + "line " + line + ", column " + column + ": " + detail);
        this.document = document;
        this.line = line;
        this.column = column;
        this.detail = detail;
    }

    /** The same refusal, naming the document. */
    public DataMapSyntaxException at(String document) {
        return new DataMapSyntaxException(document, line, column, detail);
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
