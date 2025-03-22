package org.jmouse.template.parsing;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.ParserOptions;

import java.util.function.Predicate;

public class BodyParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ParserOptions options = context.getOptions();

        if (options != null) {
            Parser           parser  = context.getParser(RootParser.class);
            Predicate<TokenCursor> stopper = options.stopCondition();
            Token            token   = cursor.peek();

            while (cursor.hasNext() && !stopper.test(cursor)) {
                Node node = parser.parse(cursor, context);
                parent.add(node);
            }

            if (stopper.test(cursor)) {
                cursor.next();
            }
        }
    }

}
