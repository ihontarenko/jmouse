package org.jmouse.el.parser;

import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.PlaceholderNode;
import org.jmouse.el.node.expression.PropertyNode;

import static org.jmouse.el.lexer.BasicToken.*;

public class PlaceholderParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        PlaceholderNode node   = new PlaceholderNode();
        PropertyParser  parser = (PropertyParser) context.getParser(PropertyParser.class);

        cursor.ensure(T_DOLLAR);
        cursor.ensure(T_OPEN_CURLY);

        if (parser.parse(cursor, context) instanceof PropertyNode property) {
            node.setProperty(property);
        }

        cursor.ensure(T_CLOSE_CURLY);

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.placeholder().matches(cursor);
    }

}
