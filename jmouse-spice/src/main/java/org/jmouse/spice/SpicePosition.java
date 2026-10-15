package org.jmouse.spice;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenizableSource;

/**
 * Where something stands in a SPICE text: its 1-based line and column, as an editor counts them.
 *
 * @param line   the 1-based line; 0 for something that was not read from a text — a node a program built
 * @param column the 1-based column; 0 when only the line is known
 */
public record SpicePosition(int line, int column) {

    /** The position of a node a program built rather than read. */
    public static final SpicePosition NONE = new SpicePosition(0, 0);

    /** A position that names a line alone. */
    public static SpicePosition ofLine(int line) {
        return new SpicePosition(line, 0);
    }

    /** Where a token stands in the source it was read from. */
    public static SpicePosition of(Token token, TokenizableSource source) {
        return new SpicePosition(token.lineNumber(), source.getColumnNumber(token.offset()));
    }

    @Override
    public String toString() {
        return column > 0 ? "line " + line + ":" + column : "line " + line;
    }
}
