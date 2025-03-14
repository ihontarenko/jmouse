package org.jmouse.template.parsing.parser;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.PropertyNode;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;

import static org.jmouse.template.lexer.BasicToken.*;

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
