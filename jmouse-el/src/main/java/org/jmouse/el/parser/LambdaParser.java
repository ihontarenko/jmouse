package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.LambdaNode;
import org.jmouse.el.node.expression.ParameterSetNode;
import org.jmouse.el.parser.sub.ParametersParser;

import static org.jmouse.el.lexer.BasicToken.*;

public class LambdaParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        LambdaNode lambda = new LambdaNode();

        cursor.ensure(T_OPEN_PAREN);

        if (cursor.isCurrent(T_IDENTIFIER)) {
            ExpressionNode parameters = (ExpressionNode) context.getParser(ParametersParser.class).parse(cursor, context);
            lambda.setParameters((ParameterSetNode) parameters);
        }

        cursor.ensure(T_CLOSE_PAREN);
        cursor.ensure(T_ARROW);

        ExpressionNode body = (ExpressionNode) context.getParser(ExpressionParser.class).parse(cursor, context);

        lambda.setBody(body);

        parent.add(lambda);
    }

}
