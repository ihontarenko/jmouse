package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.FragmentNode;

/**
 * Reads {@code fragment name { … }} — rules shared across targets, declared at file level.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.FRAGMENT)
public class FragmentParser extends JmmBlockParser<FragmentNode, JmmToken> {

    @Override
    protected FragmentNode createNode(TokenCursor cursor, ParserContext context) {
        FragmentNode fragment = new FragmentNode();

        fragment.setName(cursor.ensure(JmmToken.nameTokens()).value());

        return fragment;
    }

    @Override
    protected JmmToken token() {
        return JmmToken.T_FRAGMENT;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_FRAGMENT) && !cursor.isNext(BasicToken.T_COLON);
    }

    /**
     * Reads the rules, then refuses an include.
     *
     * <p>⚠️ Flat by design: a fragment including another would need a resolution order and could be
     * written as a cycle. Refusing it costs a line and removes both problems permanently.</p>
     */
    @Override
    protected void parseBody(TokenCursor cursor, FragmentNode node, ParserContext context) {
        Node children = parseStatements(cursor, context);

        RuleBlocks.fill(cursor, node, children);

        if (!node.getIncludes().isEmpty()) {
            throw new JmmSyntaxException(cursor,
                    "a fragment does not include another fragment — write the rules out");
        }
    }
}
