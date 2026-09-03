package org.jmouse.validator.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.validator.el.lexer.JmvToken;
import org.jmouse.validator.el.node.CheckBlockNode;
import org.jmouse.validator.el.node.CheckLineNode;

/**
 * Reads {@code gate { … }} — the block whose failure answers for the whole document.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmvParserPriority.GATE)
public class GateParser extends JmvBlockParser<CheckBlockNode, JmvToken> {

    @Override
    protected CheckBlockNode createNode(TokenCursor cursor, ParserContext context) {
        CheckBlockNode node = new CheckBlockNode();

        node.setKind(CheckBlockNode.Kind.GATE);

        return node;
    }

    @Override
    protected JmvToken token() {
        return JmvToken.T_GATE;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.opensBlock(cursor, JmvToken.T_GATE);
    }

    /**
     * Reads the body, then refuses anything that is not a check line.
     *
     * <p>⚠️ A guard inside a gate would be a condition evaluated before the record has been established
     * as worth judging, and an invariant would be an assertion about the record the gate exists to
     * doubt. Both parse perfectly well, which is exactly why this is said out loud: the framework's
     * dispatch offers every statement to every block, and a block that accepts a shape it cannot mean
     * is a block whose contents quietly do nothing.</p>
     */
    @Override
    protected void parseBody(TokenCursor cursor, CheckBlockNode node, ParserContext context) {
        super.parseBody(cursor, node, context);

        if (node.getExpressions().size() != node.getExpressions(CheckLineNode.class).size()) {
            throw new JmvSyntaxException(cursor,
                    "a 'gate' holds check lines only — a guard or an invariant inside one asks about a "
                    + "record the gate has not yet let through");
        }
    }
}
