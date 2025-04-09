package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.NamesNode;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.sub.NamesParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.FromNode;

public class FromParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(TemplateToken.T_FROM);

        ExpressionNode source = (ExpressionNode) context.getParser(LiteralParser.class)
                .parse(cursor, context);

        cursor.ensure(TemplateToken.T_IMPORT);

        NamesNode names = (NamesNode) context.getParser(NamesParser.class)
                .parse(cursor, context);

        FromNode node = new FromNode();

        node.setSource(source);
        node.setNames(names);

        return node;
    }

    @Override
    public String getName() {
        return "from";
    }

}
