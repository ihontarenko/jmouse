package org.jmouse.parallax.script.reader;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.parallax.script.ExperienceParseException;

import java.util.function.Supplier;

/**
 * {@code { statement … }} — the body of a jMX block. The engine's statement reader walks it — blank lines, {@code ;},
 * comments, the closing brace, a statement that reads nothing —, and the block's own grammar reads each statement:
 * a line of {@code parameters}, a {@code bench} with its parts inside an {@code expect}, a {@code step} of a lesson.
 *
 * <p>One per block read, since what it says names the block: {@code 'parts' is opened with '{' and never closed}.</p>
 */
final class BlockBodyParser extends StatementsParser {

    /** Reads one statement of the body, the cursor on its first token; it must consume what it reads. */
    @FunctionalInterface
    interface Statement {
        void read();
    }

    private final Reading reading;
    private final String opening;
    private final Supplier<ExperienceParseException> unclosed;
    private final Statement statement;

    /**
     * @param opening   what the opening brace is, for the refusal when it is missing: {@code '{' to open meta}
     * @param unclosed  the refusal when the text ends before the closing brace
     * @param statement reads one statement
     */
    BlockBodyParser(Reading reading, String opening, Supplier<ExperienceParseException> unclosed, Statement statement) {
        this.reading = reading;
        this.opening = opening;
        this.unclosed = unclosed;
        this.statement = statement;
    }

    /** Reads the body the cursor opens; the cursor ends after its closing brace. */
    void read(ParserContext context) {
        parse(reading.cursor, context);
    }

    @Override
    protected void open(TokenCursor cursor) {
        reading.expect(BasicToken.T_OPEN_CURLY, opening);
    }

    @Override
    protected Expression statement(TokenCursor cursor, ParserContext context) {
        if (reading.atEnd()) {
            throw unclosed.get();
        }
        statement.read();
        return new StatementNode();
    }

    /** A statement of a block in the parse tree; what it said, the block's grammar has already gathered. */
    private static final class StatementNode extends AbstractExpression {

        @Override
        public Object evaluate(EvaluationContext context) {
            return null;
        }
    }
}
