package org.jmouse.template.parser;

import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.parser.global.FunctionParser;
import org.jmouse.template.parser.global.LiteralParser;
import org.jmouse.template.parser.global.OperatorParser;

import static org.jmouse.template.lexer.BasicToken.*;

public class ExpressionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {

        if (cursor.matchesSequence(T_IDENTIFIER, T_OPEN_PAREN)) {
            context.getParser(FunctionParser.class).parse(cursor, parent, context);
        } else if (cursor.isCurrent(T_INT, T_FLOAT, T_STRING, T_TRUE, T_FALSE, T_NULL)) {
            context.getParser(LiteralParser.class).parse(cursor, parent, context);
        }

        if (cursor.isNext(T_VERTICAL_SLASH)) {
            System.out.println("ver");
        }
    }

}
