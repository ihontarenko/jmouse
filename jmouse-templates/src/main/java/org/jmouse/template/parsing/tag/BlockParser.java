package org.jmouse.template.parsing.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.template.rendering.RenderableNode;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.template.rendering.TagParser;
import org.jmouse.template.node.BlockNode;
import org.jmouse.template.parsing.TemplateParser;

import static org.jmouse.template.el.TemplateToken.*;

public class BlockParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        TemplateParser       parser  = (TemplateParser) context.getParser(TemplateParser.class);
        BlockNode            block   = new BlockNode();
        Matcher<TokenCursor> matcher = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_BLOCK);

        cursor.ensure(T_BLOCK);
        ExpressionNode name = (ExpressionNode) context.getParser(LiteralParser.class).parse(cursor, context);
        cursor.ensure(T_CLOSE_EXPRESSION);
        RenderableNode content = (RenderableNode) parser.parse(cursor, context, matcher);

        block.setName(name);
        block.setBody(content);

        return block;
    }

    @Override
    public String getName() {
        return "block";
    }

}
