package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.DoNode;

public class DoParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(TemplateToken.T_DO);

        Node expression = context.getParser(ExpressionParser.class).parse(cursor, context);

        return new DoNode((Expression) expression);
    }

    @Override
    public String getName() {
        return "do";
    }

}
