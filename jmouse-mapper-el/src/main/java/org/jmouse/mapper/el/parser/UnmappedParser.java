package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.TargetNode;
import org.jmouse.mapper.el.node.UnmappedNode;

/**
 * Reads {@code unmapped fail} or {@code unmapped ignore}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.UNMAPPED)
public class UnmappedParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        UnmappedNode node = new UnmappedNode();

        node.setSpan(JmmSpans.at(cursor));

        cursor.ensure(JmmToken.T_UNMAPPED);

        if (cursor.consumeIf(JmmToken.T_FAIL)) {
            node.setValue(TargetNode.Unmapped.FAIL);
        } else if (cursor.consumeIf(JmmToken.T_IGNORE)) {
            node.setValue(TargetNode.Unmapped.IGNORE);
        } else {
            throw new JmmSyntaxException(cursor, "'unmapped' takes 'fail' or 'ignore'");
        }

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_UNMAPPED) && !cursor.isNext(BasicToken.T_COLON);
    }
}
