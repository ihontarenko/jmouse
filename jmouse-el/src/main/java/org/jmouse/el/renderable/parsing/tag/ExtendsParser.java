package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.core.parser.LiteralParser;
import org.jmouse.el.core.parser.ParserContext;
import org.jmouse.el.core.parser.TagParser;
import org.jmouse.el.core.rendering.RenderableNode;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.ExtendsNode;

public class ExtendsParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(TemplateToken.T_EXTENDS);

        ExtendsNode    node  = new ExtendsNode();
        ExpressionNode value = (ExpressionNode) context.getParser(LiteralParser.class).parse(cursor, context);

        node.setParent(value);

        return node;
    }

    @Override
    public String getName() {
        return "extends";
    }

}
