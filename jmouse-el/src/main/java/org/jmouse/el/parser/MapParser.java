package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.MapNode;
import org.jmouse.el.parser.sub.KeyValueParser;

public class MapParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Node map = new MapNode();

        cursor.ensure(BasicToken.T_OPEN_CURLY);
        if (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            context.getParser(KeyValueParser.class).parse(cursor, map, context);
        }
        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        parent.add(map);
    }
}
