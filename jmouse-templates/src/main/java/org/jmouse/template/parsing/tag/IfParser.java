package org.jmouse.template.parsing.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.RenderableNode;
import org.jmouse.el.parser.*;
import org.jmouse.template.TemplateToken;
import org.jmouse.template.node.IfNode;
import org.jmouse.template.parsing.BodyParser;

public class IfParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        // consume 'if'
        cursor.ensure(TemplateToken.T_IF);
        IfNode         ifNode = new IfNode();
        Parser         parser = context.getParser(ExpressionParser.class);
        ExpressionNode condition   = (ExpressionNode) parser.parse(cursor, context);

        cursor.ensure(TemplateToken.T_CLOSE_EXPRESSION);

        while (true) {
            ParserOptions options = ParserOptions.withStopCondition(c -> c.matchesSequence(TemplateToken.T_ELSE_IF) || c.matchesSequence(TemplateToken.T_OPEN_EXPRESSION, TemplateToken.T_ELSE) || c.matchesSequence(TemplateToken.T_ELSE_IF));
            context.setOptions(options);
            context.getParser(BodyParser.class).parse(cursor, context);
            System.out.println("qwerqweqwe");
            break;
        }

        return ifNode;
    }

    @Override
    public String getName() {
        return "if";
    }

}
