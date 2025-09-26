package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.ExtendsNode;

public class ExtendsParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(TemplateToken.T_EXTENDS);

        ExtendsNode    node  = new ExtendsNode();
        Expression value = (Expression) context.getParser(LiteralParser.class)
                .parse(cursor, context);

        node.setPath(value);

        return node;
    }

    @Override
    public String getName() {
        return "extends";
    }

}
