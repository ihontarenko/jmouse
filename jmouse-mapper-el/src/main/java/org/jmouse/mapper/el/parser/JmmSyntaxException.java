package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.expression.SpanNode;

/**
 * A {@code .jmm} file that cannot be read, said in a way the person who wrote it can act on. 📍
 *
 * <h2>⚠️ The line is not decoration</h2>
 *
 * <p>A parse failure is read by somebody who did not write the parser. "Unexpected token" names
 * nothing they can find; a file, a line and a column name the place they have to look, and the whole
 * reason this language exists is that a mapping should be editable by somebody who is not editing
 * Java.</p>
 *
 * <p>A cursor is allowed not to know its source. Where that happens the offset stands in for the
 * column — degraded, never absent, because a failure that reports nothing at all is the one thing
 * worse than a failure reporting a number that is only approximately right.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JmmSyntaxException extends RuntimeException {

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
    public JmmSyntaxException(TokenCursor cursor, String message) {
        this(cursor, cursor == null ? null : cursor.current(), message);
    }

    /**
     * Raises a failure pointing at a specific token.
     *
     * @param cursor  the cursor the token was read from
     * @param token   the token to point at
     * @param message what is wrong
     */
    public JmmSyntaxException(TokenCursor cursor, Token token, String message) {
        this(null, token == null ? 0 : token.lineNumber(),
             cursor == null || token == null ? 0 : SourceReading.column(cursor, token), message);
    }

    /**
     * Raises a failure at a known place, once the file it came from is known.
     *
     * @param source     the file, or {@code null} while it is not yet known
     * @param lineNumber the 1-based line
     * @param column     the 1-based column
     * @param message    what is wrong
     */
    public JmmSyntaxException(String source, int lineNumber, int column, String message) {
        this(source, lineNumber, column, message, null);
    }

    /**
     * Raises a failure at a known place, keeping what caused it.
     *
     * <h3>⚠️ The cause is not decoration either</h3>
     *
     * <p>Where this wraps another failure — the expression compiler refusing a rule's value, most of the
     * time — its message is folded into this one and used to be all that survived. The stack underneath
     * it went, and with it the only thing that says <em>where in the compiler</em> the refusal came from.
     * A message can say what is wrong with a file; a cause is what says why a compiler thought so.</p>
     *
     * @param source     the file, or {@code null} while it is not yet known
     * @param lineNumber the 1-based line
     * @param column     the 1-based column
     * @param message    what is wrong
     * @param cause      what raised it, or {@code null}
     */
    public JmmSyntaxException(String source, int lineNumber, int column, String message, Throwable cause) {
        super(describe(source, lineNumber, column, message), cause);
        this.source = source;
        this.lineNumber = lineNumber;
        this.column = column;
        this.problem = message;
    }

    /**
     * Raises a failure at the place a node was written.
     *
     * <h3>⚠️ Why a span rather than a cursor</h3>
     *
     * <p>A cursor knows where it is; a node knows where it <em>was</em>. Everything past the parser —
     * type resolution, validation against a class, compiling a value — happens with no cursor anywhere,
     * and the position it needs was stamped onto the node while one still existed. Failures raised there
     * used to report {@code 0:0}, which reads as a file with no lines in it.</p>
     *
     * @param span    where the construction was written, or {@code null} when nothing stamped one
     * @param message what is wrong
     * @return the failure, positioned as well as it can be
     */
    public static JmmSyntaxException at(SpanNode span, String message) {
        return at(span, message, null);
    }

    /**
     * The same, keeping what caused it.
     *
     * @param span    where the construction was written, or {@code null}
     * @param message what is wrong
     * @param cause   what raised it, or {@code null}
     * @return the failure
     */
    public static JmmSyntaxException at(SpanNode span, String message, Throwable cause) {
        int lineNumber = span == null ? 0 : span.lineNumber();
        int column     = span == null ? 0 : span.position();

        return new JmmSyntaxException(null, lineNumber, column, message, cause);
    }

    /**
     * The same failure, knowing which file it came from.
     *
     * <p>⚠️ A parser reads a cursor and has no idea what it was opened from — the name belongs to
     * whoever loaded it. Rather than thread a file name through every parser so that one message can
     * carry it, the loader re-stamps the failure on its way out.</p>
     *
     * @param file the file this came from
     * @return an equivalent failure naming the file
     */
    public JmmSyntaxException at(String file) {
        return new JmmSyntaxException(file, lineNumber, column, problem, getCause());
    }

    /** @return the file this came from, or {@code null} when it was never stamped */
    public String source() {
        return source;
    }

    /** @return the 1-based line */
    public int lineNumber() {
        return lineNumber;
    }

    /** @return the 1-based column, or the raw offset when the cursor had no source */
    public int column() {
        return column;
    }

    /**
     * What is wrong, without the location.
     *
     * <p>⚠️ Kept as a field rather than cut back out of {@link #getMessage()}. Re-stamping a failure
     * with a file name has to rebuild the sentence, and recovering the problem by looking for the
     * separator would stack two locations the first time a message itself contained one — which the
     * expression compiler's messages routinely do.</p>
     *
     * @return what is wrong, on its own
     */
    public String problem() {
        return problem;
    }

    /**
     * Renders the location and the problem as one sentence.
     *
     * @param source     the file, or {@code null}
     * @param lineNumber the 1-based line
     * @param column     the 1-based column
     * @param message    what is wrong
     * @return the full message
     */
    private static String describe(String source, int lineNumber, int column, String message) {
        String where = source == null ? "line %d:%d".formatted(lineNumber, column)
                                      : "%s:%d:%d".formatted(source, lineNumber, column);
        return "%s: %s".formatted(where, message);
    }
}
