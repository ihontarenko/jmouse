package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.core.parser.LiteralParser;
import org.jmouse.el.core.parser.ParserContext;
import org.jmouse.el.core.parser.TagParser;
import org.jmouse.el.renderable.RenderableNode;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.IncludeNode;

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
