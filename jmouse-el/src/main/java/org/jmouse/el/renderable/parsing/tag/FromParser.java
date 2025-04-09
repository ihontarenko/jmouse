package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;

public class FromParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(TemplateToken.T_FROM);

        ExpressionNode from = (ExpressionNode) context.getParser(ExpressionParser.class).parse(cursor, context);

        return null;
    }

    @Override
    public String getName() {
        return "from";
    }

}
