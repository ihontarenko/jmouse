package org.jmouse.template.parser.core;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.literal.*;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

import static org.jmouse.template.lexer.BasicToken.*;

public class LiteralParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Token      token = cursor.peek();
        BasicToken type  = (BasicToken) token.type();

        cursor.ensure(T_INT, T_FLOAT, T_STRING, T_TRUE, T_FALSE, T_NULL);

        switch (type) {
            case T_NULL:
                parent.add(new NullLiteralNode());
                break;
            case T_STRING:
                parent.add(new StringLiteralNode(token.value()));
                break;
            case T_INT:
                parent.add(new LongLiteralNode(Long.parseLong(token.value())));
                break;
            case T_FLOAT:
                parent.add(new DoubleLiteralNode(Double.parseDouble(token.value())));
                break;
            case T_TRUE:
            case T_FALSE:
                parent.add(new BooleanLiteralNode(type == T_TRUE));
                break;
        }

        cursor.next();
    }

}
