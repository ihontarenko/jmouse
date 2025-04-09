package org.jmouse.el.parser.sub;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ParameterNode;
import org.jmouse.el.node.expression.ParametersNode;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;

public class ParametersParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ParametersNode parameters = new ParametersNode();
        Parser         parser     = context.getParser(ExpressionParser.class);

        do {
            ParameterNode parameter = new ParameterNode();
            Token         token     = cursor.ensure(BasicToken.T_IDENTIFIER);

            parameter.setName(token.value());

            if (cursor.currentIf(BasicToken.T_COLON)) {
                parameter.setDefaultValue((ExpressionNode) parser.parse(cursor, context));
            }

            parameters.addParameter(parameter);
        } while (cursor.isCurrent(BasicToken.T_COMMA) && cursor.next() != null);

        parent.add(parameters);
    }

}
