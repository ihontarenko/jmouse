package org.jmouse.template.parsing.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.rendering.RenderableNode;
import org.jmouse.template.lexer.TemplateToken;
import org.jmouse.template.node.IncludeNode;

public class IncludeParser implements TagParser {
    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(TemplateToken.T_INCLUDE);
        return new IncludeNode((ExpressionNode) context.getParser(LiteralParser.class).parse(cursor, context));
    }

    @Override
    public String getName() {
        return "include";
    }
}
