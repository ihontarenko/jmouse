package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.UseNode;

/**
 * Reads {@code use fully.qualified.Type} — brings a type into the file under its simple name.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.USE)
public class UseParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        UseNode node = new UseNode();

        node.setSpan(JmmSpans.at(cursor));

        cursor.ensure(JmmToken.T_USE);
        node.setQualifiedName(TypeNames.read(cursor));

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_USE) && !cursor.isNext(BasicToken.T_COLON);
    }
}
