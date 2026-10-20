package org.jmouse.parallax.script.reader;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

/**
 * Reads one line of a jMX block from the cursor — a parameter, a part, a pin — into what it declares, and adds it to
 * the tree as a {@link LineNode}. A block reads its body with the engine's statement reader and each statement with
 * its line parser ({@link Reading#lines}).
 *
 * <p>⚠️ Never chosen by the engine's dispatch ({@link #supports} is false): a line is a parameter because it stands in
 * {@code parameters}, not because of how it begins.</p>
 *
 * @param <T> what a line declares
 */
abstract class JmxLineParser<T> extends AbstractParser {

    /** Reads the line the cursor stands on; the cursor is left at its end. */
    abstract T read();

    @Override
    public final void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(new LineNode<>(read()));
    }

    @Override
    public final boolean supports(TokenCursor cursor) {
        return false;
    }

    /** A line of a block in the parse tree, holding what it declares. */
    static final class LineNode<T> extends AbstractExpression {

        private final T declared;

        LineNode(T declared) {
            this.declared = declared;
        }

        T declared() {
            return declared;
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return declared;
        }
    }
}
