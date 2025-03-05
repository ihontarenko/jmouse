package org.jmouse.template.parser.expression;

import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.RenderableNode;
import org.jmouse.template.parser.ExpressionParser;
import org.jmouse.template.parser.ParserContext;

public class IfParser implements ExpressionParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        return null;
    }

    @Override
    public String getName() {
        return "if";
    }

}
