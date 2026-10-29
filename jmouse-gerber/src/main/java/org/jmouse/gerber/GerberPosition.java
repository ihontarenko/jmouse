package org.jmouse.gerber;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenizableSource;

/**
 * Where something stands in a Gerber or an Excellon text: its 1-based line and column, as an editor counts them.
 *
 * @param line   the 1-based line; 0 for something that was not read from a text — a node a program built
 * @param column the 1-based column; 0 when only the line is known
 */
public record GerberPosition(int line, int column) {

    /** The position of a node a program built rather than read. */
    public static final GerberPosition NONE = new GerberPosition(0, 0);

    /** Where a token stands in the source it was read from. */
    public static GerberPosition of(Token token, TokenizableSource source) {
        return new GerberPosition(token.lineNumber(), source.getColumnNumber(token.offset()));
    }

    @Override
    public String toString() {
        return column > 0 ? "line " + line + ":" + column : "line " + line;
    }
}
