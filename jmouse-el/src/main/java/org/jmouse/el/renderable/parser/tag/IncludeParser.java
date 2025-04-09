package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.IncludeNode;

public class IncludeParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(TemplateToken.T_INCLUDE);
        return new IncludeNode((ExpressionNode) context.getParser(LiteralParser.class).parse(cursor, context));
    }

    @Override
    public String getName() {
        return "include";
    }
}
