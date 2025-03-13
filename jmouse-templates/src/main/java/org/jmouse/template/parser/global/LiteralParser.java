package org.jmouse.template.parser.global;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.LiteralNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

import static org.jmouse.template.lexer.BasicToken.*;
import static org.jmouse.template.lexer.BasicToken.T_NULL;

public class LiteralParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Token token = cursor.peek();

        cursor.ensure(T_INT, T_FLOAT, T_STRING, T_TRUE, T_FALSE, T_NULL);
        parent.add(new LiteralNode(token.value()));

        cursor.next();
    }

}
