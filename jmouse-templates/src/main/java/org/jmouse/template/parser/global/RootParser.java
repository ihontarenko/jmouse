package org.jmouse.template.parser.global;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.RawTextNode;
import org.jmouse.template.parser.ParseException;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;
import org.jmouse.template.parser.arithmetic.ArithmeticParser;

import static org.jmouse.template.lexer.BasicToken.*;
import static org.jmouse.template.lexer.TemplateToken.*;

public class RootParser implements Parser {

    public static final String NAME = "root";

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Token token;

        while (!cursor.hasNext()) {
            token = cursor.peek();

            if (cursor.isCurrent(T_RAW_TEXT)) {
                // consume current token as raw text
                parent.add(new RawTextNode(token.value()));
                cursor.next();
            } else if (cursor.isCurrent(T_OPEN_PRINT)) {
                // advance open braces "{{"
                cursor.expect(T_OPEN_PRINT);
            } else if (cursor.isCurrent(T_OPEN_EXPRESSION)) {
                // advance open braces "{%"
                cursor.expect(T_OPEN_EXPRESSION);

                if (!cursor.isCurrent(T_IDENTIFIER)) {
                    throw new ParseException("Expression block must start with an identifier but was " + token);
                }

                Token identifier = cursor.expect(T_IDENTIFIER);

            } else {
                cursor.expect(T_EOL);
            }
        }

    }

    public void parseExpression(TokenCursor cursor, Node parent, ParserContext context) {

        if (cursor.matchesSequence(T_IDENTIFIER, T_OPEN_PAREN)) {
            context.getParser(FunctionParser.NAME).parse(cursor, context);
        } else if (cursor.matchesSequence(T_IDENTIFIER, T_DOT)) {
            // property access
        } else if (cursor.isNext(T_PLUS, T_MINUS, T_MULTIPLY, T_DIVIDE, T_CARET)) {
            context.getParser(ArithmeticParser.NAME).parse(cursor, parent, context);
        }

    }

    @Override
    public String getName() {
        return NAME;
    }

}
