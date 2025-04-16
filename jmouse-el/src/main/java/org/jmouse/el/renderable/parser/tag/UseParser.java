package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.node.UseNode;

import static org.jmouse.el.renderable.lexer.TemplateToken.T_USE;

public class UseParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(T_USE);

        Node path = context.getParser(ExpressionParser.class).parse(cursor, context);

        UseNode use = new UseNode();

        use.setPath((ExpressionNode) path);

        return use;
    }

    @Override
    public String getName() {
        return "use";
    }

}
