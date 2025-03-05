package org.jmouse.template.parser.global;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.expression.FunctionNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

public class FunctionParser implements Parser {

    public static final String NAME = "function";

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Node function = new FunctionNode(cursor.peek().value());

        cursor.expect(BasicToken.T_OPEN_PAREN);

        if (!cursor.isNext(BasicToken.T_CLOSE_PAREN)) {
            // parse arguments
        }

        cursor.expect(BasicToken.T_CLOSE_PAREN);

        parent.add(function);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
