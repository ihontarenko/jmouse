package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
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
        ExpressionNode value = (ExpressionNode) context.getParser(LiteralParser.class)
                .parse(cursor, context);

        node.setParent(value);

        return node;
    }

    @Override
    public String getName() {
        return "extends";
    }

}
