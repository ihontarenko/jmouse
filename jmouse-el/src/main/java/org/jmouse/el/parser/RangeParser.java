package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.RangeNode;

import static org.jmouse.el.lexer.BasicToken.T_DOUBLE_DOT;
import static org.jmouse.el.lexer.BasicToken.T_INT;

public class RangeParser implements Parser{

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        String start = cursor.ensure(T_INT).value();
        cursor.ensure(T_DOUBLE_DOT);
        String end = cursor.ensure(T_INT).value();

        RangeNode node = new RangeNode();

        node.setStart(Integer.parseInt(start));
        node.setEnd(Integer.parseInt(end));

        parent.add(node);
    }

}
