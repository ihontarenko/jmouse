package org.jmouse.validator.el.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;

/**
 * A {@code .jmv} file that cannot be read, said in a way the person who wrote it can act on. 📍
 *
 * <h2>⚠️ The line is not decoration</h2>
 *
 * <p>A parse failure is read by somebody who did not write the parser. "Unexpected token" names nothing
 * they can find; a file, a line and a column name the place they have to look — and the whole reason
 * this language exists is that validation should be editable by somebody who is not editing Java.</p>
 *
 * <p>A cursor is allowed not to know its source. Where that happens the offset stands in for the
 * column — degraded, never absent, because a failure reporting nothing at all is the one thing worse
 * than one reporting a number that is only approximately right.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JmvSyntaxException extends RuntimeException {

    private final String source;
    private final int    lineNumber;
    private final int    column;
    private final String problem;

    /**
     * Raises a failure pointing at wherever the cursor currently sits.
     *
     * @param cursor  the cursor, positioned at the problem
     * @param message what is wrong, in the words of somebody reading the file
     */
    public JmvSyntaxException(TokenCursor cursor, String message) {
        this(cursor, cursor == null ? null : cursor.current(), message);
    }

    /**
     * Raises a failure pointing at a specific token.
     *
     * @param cursor  the cursor the token was read from
     * @param token   the token to point at
     * @param message what is wrong
     */
    public JmvSyntaxException(TokenCursor cursor, Token token, String message) {
        this(null,
             token == null ? 0 : token.lineNumber(),
             cursor == null || token == null ? 0 : SourceReading.column(cursor, token),
             message);
    }

    /**
     * Raises a failure at a known place.
     *
     * @param source     the file, or {@code null} while it is not yet known
     * @param lineNumber the 1-based line
     * @param column     the 1-based column
     * @param message    what is wrong
     */
    public JmvSyntaxException(String source, int lineNumber, int column, String message) {
        super(compose(source, lineNumber, column, message));
        this.source = source;
        this.lineNumber = lineNumber;
        this.column = column;
        this.problem = message;
    }

    /**
     * The same failure, now knowing which file it came from.
     *
     * <p>⚠️ The parser reads a cursor and has no idea what it was opened from. Threading a file name
     * through every parser so one message can carry it would put a parameter nobody uses in every
     * signature; the reader stamps it here instead, on the way out.</p>
     *
     * @param file what to call the file
     * @return a failure carrying the same place and problem, plus the file
     */
    public JmvSyntaxException at(String file) {
        return new JmvSyntaxException(file, lineNumber, column, problem);
    }

    /** @return the file, or {@code null} when it was never stamped */
    public String source() {
        return source;
    }

    /** @return the 1-based line the problem is on */
    public int lineNumber() {
        return lineNumber;
    }

    /** @return the 1-based column, or an offset standing in for one */
    public int column() {
        return column;
    }

    /** @return what is wrong, without the place */
    public String problem() {
        return problem;
    }

    private static String compose(String source, int lineNumber, int column, String message) {
        return "%s:%d:%d — %s".formatted(source == null ? "<jmv>" : source, lineNumber, column, message);
    }
}
