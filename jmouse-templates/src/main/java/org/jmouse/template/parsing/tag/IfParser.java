package org.jmouse.template.parsing.tag;

import org.jmouse.template.lexer.TemplateToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.ExpressionNode;
import org.jmouse.template.node.RenderableNode;
import org.jmouse.template.node.renderable.IfNode;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;
import org.jmouse.template.parsing.TagParser;
import org.jmouse.template.parsing.parser.OperatorParser;

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
