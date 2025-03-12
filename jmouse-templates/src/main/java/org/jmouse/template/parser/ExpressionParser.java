package org.jmouse.template.parser;

import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.parser.global.FunctionParser;
import org.jmouse.template.parser.global.OperatorParser;

import static org.jmouse.template.lexer.BasicToken.*;
import static org.jmouse.template.lexer.BasicToken.T_CARET;

public class ExpressionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (cursor.matchesSequence(T_IDENTIFIER, T_OPEN_PAREN)) {
            context.getParser(FunctionParser.class).parse(cursor, context);
        } else if (cursor.matchesSequence(T_IDENTIFIER, T_DOT)) {
            // property access
        } else if (cursor.isNext(T_PLUS, T_MINUS, T_MULTIPLY, T_DIVIDE, T_CARET)) {
            context.getParser(OperatorParser.class).parse(cursor, parent, context);
        } else {

        }
    }

}
