package org.jmouse.template.parsing.tag;

import org.jmouse.template.TemplateToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.RenderableNode;
import org.jmouse.template.node.renderable.IfNode;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.OperatorParser;

public class IfParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        // consume 'if'
        cursor.ensure(TemplateToken.T_IF);

        Parser         parser = context.getParser(OperatorParser.class);
        ExpressionNode node   = (ExpressionNode) parser.parse(cursor, context);

        cursor.ensure(TemplateToken.T_CLOSE_EXPRESSION);

        System.out.println(cursor.current());

        return new IfNode();
    }

    @Override
    public String getName() {
        return "if";
    }

}
