package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.core.CursorMatcher;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.renderable.RenderableNode;
import org.jmouse.el.core.parser.LiteralParser;
import org.jmouse.el.core.parser.ParserContext;
import org.jmouse.el.core.parser.TagParser;
import org.jmouse.el.renderable.node.BlockNode;
import org.jmouse.el.renderable.parsing.TemplateParser;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

public class BlockParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        TemplateParser       parser  = (TemplateParser) context.getParser(TemplateParser.class);
        BlockNode            block   = new BlockNode();
        Matcher<TokenCursor> matcher = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_BLOCK);

        cursor.ensure(T_BLOCK);
        ExpressionNode name = (ExpressionNode) context.getParser(LiteralParser.class).parse(cursor, context);
        cursor.ensure(T_CLOSE_EXPRESSION);
        RenderableNode body = (RenderableNode) parser.parse(cursor, context, matcher);

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
