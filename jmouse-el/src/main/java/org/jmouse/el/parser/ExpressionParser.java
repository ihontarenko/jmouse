package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.FilterNode;

import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_VERTICAL_SLASH;

public class ExpressionParser implements Parser {

    /**
     * Parses an operation tag and adds it as a child of the given parent node.
     *
     * @param cursor  the token cursor
     * @param parent  the parent node to attach the parsed tag
     * @param context the parser evaluation
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        OperatorParser operatorParser = (OperatorParser) context.getParser(OperatorParser.class);
        ExpressionNode left           = (ExpressionNode) operatorParser.parse(cursor, context);

        // parse right expression if present
        while (cursor.hasNext() && cursor.matchesSequence(T_VERTICAL_SLASH, T_IDENTIFIER)) {
            cursor.expect(T_IDENTIFIER);
            Node right = context.getParser(FilterParser.class).parse(cursor, context);
            if (right instanceof FilterNode filter) {
                filter.setLeft(left);
                left = filter;
            }
        }

        parent.add(left);
    }

}
