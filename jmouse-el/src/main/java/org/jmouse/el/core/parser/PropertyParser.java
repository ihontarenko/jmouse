package org.jmouse.el.core.parser;

import org.jmouse.el.core.lexer.Token;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.core.node.expression.PropertyNode;

import static org.jmouse.el.core.lexer.BasicToken.*;

public class PropertyParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Token         token   = cursor.next();
        StringBuilder builder = new StringBuilder();

        builder.append(token.value());

        while (cursor.isCurrent(T_DOT, T_OPEN_BRACKET)) {
            boolean openBracket = cursor.isCurrent(T_OPEN_BRACKET);
            builder.append(cursor.peek().value());
            cursor.next();
            builder.append(cursor.peek().value());
            cursor.next();

            if (openBracket) {
                builder.append(cursor.peek().value());
                cursor.ensure(T_CLOSE_BRACKET);
            }
        }

        parent.add(new PropertyNode(builder.toString()));
    }

}
