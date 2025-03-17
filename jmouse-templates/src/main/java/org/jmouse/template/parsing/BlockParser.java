package org.jmouse.template.parsing;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parsing.Parser;
import org.jmouse.el.parsing.ParserContext;
import org.jmouse.el.parsing.ParserOptions;

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
