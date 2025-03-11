package org.jmouse.template.parser.global;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

import java.util.function.Predicate;

public class BlockParser implements Parser {

    public static final String NAME = "block";

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (context.getOptions() != null) {
            Parser           parser  = context.getParser(RootParser.class);
            Predicate<Token> stopper = context.getOptions().stopCondition();
            Token            token   = cursor.peek();
            while (cursor.hasNext() && !stopper.test(token)) {
                cursor.next();
                parent.add(parser.parse(cursor, context));
            }
        }
    }

}
