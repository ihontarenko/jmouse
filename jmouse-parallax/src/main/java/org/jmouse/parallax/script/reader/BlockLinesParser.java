package org.jmouse.parallax.script.reader;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;

import java.util.List;

/**
 * {@code { line … }} — the body of a jMX block whose statements are lines: {@code parameters}, {@code parts},
 * {@code nets} and their kin. The engine's statement reader walks the body — blank lines, {@code ;}, comments, the
 * closing brace —, and each statement is one {@link Reading.Line} for the block's own grammar to read.
 *
 * <p>One per block read, since a refusal names the block: {@code 'parts' is opened with '{' and never closed}.</p>
 */
final class BlockLinesParser extends StatementsParser {

    private final Reading reading;
    private final String block;

    BlockLinesParser(Reading reading, String block) {
        this.reading = reading;
        this.block = block;
    }

    /** The lines of the block the cursor opens; the cursor ends after its closing brace. */
    List<Reading.Line> lines(ParserContext context) {
        Node body = parse(reading.cursor, context);
        return body.getChildren().stream().map(node -> ((LineNode) node).line()).toList();
    }

    @Override
    protected void open(TokenCursor cursor) {
        reading.expect(BasicToken.T_OPEN_CURLY, "'{' to open " + block);
    }

    @Override
    protected Expression statement(TokenCursor cursor, ParserContext context) {
        if (reading.atEnd()) {
            throw reading.refusal("'" + block + "' is opened with '{' and never closed");
        }
        if (cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            throw reading.refusal("'" + block + "' holds lines, not a nested '{ … }'");
        }
        return new LineNode(reading.line());
    }

    /** One line of a block in the parse tree. */
    static final class LineNode extends AbstractExpression {

        private final Reading.Line line;

        LineNode(Reading.Line line) {
            this.line = line;
        }

        Reading.Line line() {
            return line;
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return line;
        }
    }
}
