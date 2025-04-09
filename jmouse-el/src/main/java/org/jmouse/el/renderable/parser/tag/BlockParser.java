package org.jmouse.el.renderable.parser.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.node.BlockNode;
import org.jmouse.el.renderable.parser.TemplateParser;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

public class BlockParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        TemplateParser       parser  = (TemplateParser) context.getParser(TemplateParser.class);
        BlockNode            block   = new BlockNode();
        Matcher<TokenCursor> matcher = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_BLOCK);

        cursor.ensure(T_BLOCK);
        ExpressionNode name = (ExpressionNode) context.getParser(LiteralParser.class).parse(cursor, context);
        cursor.ensure(T_CLOSE_EXPRESSION);
        Node body = parser.parse(cursor, context, matcher);

        block.setName(name);
        block.setBody(body);

        cursor.ensure(T_END_BLOCK);

        return block;
    }

    @Override
    public String getName() {
        return "block";
    }

}
