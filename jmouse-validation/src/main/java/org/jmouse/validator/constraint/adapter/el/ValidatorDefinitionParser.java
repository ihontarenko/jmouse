package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.sub.KeyValueParser;

public class ValidatorDefinitionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(BasicToken.T_AT);

        Node node = new ValidationDefinitionNode(
                cursor.ensure(BasicToken.T_IDENTIFIER).value()
        );

        cursor.ensure(BasicToken.T_OPEN_PAREN);
        if (!cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
            context.getParser(KeyValueParser.class).parse(cursor, node, context);
        }
        cursor.ensure(BasicToken.T_CLOSE_PAREN);

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.matchesSequence(BasicToken.T_AT, BasicToken.T_IDENTIFIER);
    }

}
