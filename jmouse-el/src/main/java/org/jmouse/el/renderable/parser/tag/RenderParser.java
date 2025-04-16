package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.literal.StringLiteralNode;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.RenderNode;

public class RenderParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(TemplateToken.T_RENDER);

        Parser     parser = context.getParser(LiteralParser.class);
        RenderNode render = new RenderNode();

        if (cursor.isCurrent(BasicToken.T_IDENTIFIER)) {
            String name = cursor.ensure(BasicToken.T_IDENTIFIER).value();
            render.setName(new StringLiteralNode(name));
        } else if (parser.parse(cursor, context) instanceof StringLiteralNode literal) {
            render.setName(literal);
        }

        return render;
    }

    @Override
    public String getName() {
        return "render";
    }

}
