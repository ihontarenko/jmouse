package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.TernaryNode;

import static org.jmouse.el.lexer.BasicToken.*;

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
        Expression     left           = (Expression) operatorParser.parse(cursor, context);

        // ternary conditional “? trueExpression : falseExpression”
        if (cursor.currentIf(T_QUESTION)) {
            TernaryNode ternary = new TernaryNode();
            ternary.setCondition(left);
            ternary.setThenBranch((Expression) parse(cursor, context));
            cursor.ensure(T_COLON);
            ternary.setElseBranch((Expression) parse(cursor, context));
            left = ternary;
        }

        parent.add(left);
    }

}
