package org.jmouse.template.parsing.parser;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;
import org.jmouse.template.parsing.ParserOptions;

import java.util.function.Predicate;

public class BlockParser implements Parser {

    public static final String NAME = "block";

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ParserOptions options = context.getOptions();

        if (options != null) {
            Parser           parser  = context.getParser(RootParser.class);
            Predicate<Token> stopper = options.stopCondition();
            Token            token   = cursor.peek();

            while (cursor.hasNext() && !stopper.test(token)) {
                cursor.next();
                parent.add(parser.parse(cursor, context));
            }

            if (stopper.test(token)) {
                cursor.next();
            }
        }
    }

}
