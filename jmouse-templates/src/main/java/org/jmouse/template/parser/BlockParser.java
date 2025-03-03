package org.jmouse.template.parser;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;

import java.util.function.Predicate;

public class BlockParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (context.getOptions() != null) {
            Predicate<Token> stopCondition = context.getOptions().stopCondition();
            while (cursor.hasNext() && !stopCondition.test(cursor.peek())) {
                cursor.next();
                context.getParser(RootParser.class).parse(cursor, parent, context);
            }
        }
    }

}
