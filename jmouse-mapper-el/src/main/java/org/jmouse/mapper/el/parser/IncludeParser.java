package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.IncludeNode;

/**
 * Reads {@code include fragmentName} — pulls a fragment's rules into the block it is written in.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.INCLUDE)
public class IncludeParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        IncludeNode include = new IncludeNode();

        include.setSpan(JmmSpans.at(cursor));

        cursor.ensure(JmmToken.T_INCLUDE);
        include.setName(cursor.ensure(JmmToken.nameTokens()).value());

        parent.add(include);
    }

    /**
     * Whether this line pulls in a fragment — {@code include x} rather than a property called
     * {@code include}.
     */
    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_INCLUDE)
               && cursor.isNext(JmmToken.nameTokens())
               && !cursor.checkAt(2, BasicToken.T_COLON);
    }
}
