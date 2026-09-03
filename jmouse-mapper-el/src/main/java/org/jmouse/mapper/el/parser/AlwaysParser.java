package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.RuleBlockNode;

/**
 * Reads {@code always { … }} — the rules that hold whatever the source is.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.ALWAYS)
public class AlwaysParser extends JmmBlockParser<RuleBlockNode, JmmToken> {

    @Override
    protected RuleBlockNode createNode(TokenCursor cursor, ParserContext context) {
        return new RuleBlockNode();
    }

    @Override
    protected JmmToken token() {
        return JmmToken.T_ALWAYS;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_ALWAYS) && !cursor.isNext(BasicToken.T_COLON);
    }

    @Override
    protected void parseBody(TokenCursor cursor, RuleBlockNode node, ParserContext context) {
        Node children = parseStatements(cursor, context);

        RuleBlocks.fill(cursor, node, children);
    }
}
