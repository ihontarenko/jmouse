package org.jmouse.validator.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.validator.el.lexer.JmvToken;
import org.jmouse.validator.el.node.CheckBlockNode;

/**
 * Reads {@code always { … }} — the unguarded block.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmvParserPriority.ALWAYS)
public class AlwaysParser extends JmvBlockParser<CheckBlockNode, JmvToken> {

    @Override
    protected CheckBlockNode createNode(TokenCursor cursor, ParserContext context) {
        CheckBlockNode node = new CheckBlockNode();

        node.setKind(CheckBlockNode.Kind.ALWAYS);

        return node;
    }

    @Override
    protected JmvToken token() {
        return JmvToken.T_ALWAYS;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.opensBlock(cursor, JmvToken.T_ALWAYS);
    }
}
