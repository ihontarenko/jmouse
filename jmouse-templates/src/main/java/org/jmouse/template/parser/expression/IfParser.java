package org.jmouse.template.parser.expression;

import org.jmouse.template.lexer.TemplateToken;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.RenderableNode;
import org.jmouse.template.parser.ExpressionParser;
import org.jmouse.template.parser.ParserContext;

public class IfParser implements ExpressionParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        Token token = cursor.ensure(TemplateToken.T_IF);

        return null;
    }

    @Override
    public String getName() {
        return "if";
    }

}
