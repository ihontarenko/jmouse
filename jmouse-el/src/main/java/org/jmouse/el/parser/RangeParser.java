package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.RangeNode;

import static org.jmouse.el.lexer.BasicToken.*;

public class RangeParser implements Parser{

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        String start = cursor.ensure(T_NUMERIC).value();
        cursor.ensure(T_DOUBLE_DOT);
        String end = cursor.ensure(T_NUMERIC).value();

        RangeNode node = new RangeNode();

        node.setStart(Integer.parseInt(start));
        node.setEnd(Integer.parseInt(end));

        parent.add(node);
    }

}
