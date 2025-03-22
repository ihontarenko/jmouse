package org.jmouse.template.parsing.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.RenderableNode;
import org.jmouse.el.parser.*;
import org.jmouse.template.TemplateToken;
import org.jmouse.template.node.IfNode;
import org.jmouse.template.parsing.BodyParser;

import static org.jmouse.template.TemplateToken.*;

public class IfParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        // consume 'if'
        cursor.ensure(TemplateToken.T_IF);
        IfNode         ifNode = new IfNode();
        Parser         parser = context.getParser(ExpressionParser.class);
        ExpressionNode condition   = (ExpressionNode) parser.parse(cursor, context);

        cursor.ensure(TemplateToken.T_CLOSE_EXPRESSION);

        Matcher<TokenCursor> matcher  = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_ELSE);
        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_ELSE_IF));
        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_IF));

        while (true) {
            ParserOptions options = ParserOptions.withStopCondition(c -> c.matchesSequence(TemplateToken.T_ELSE_IF) || c.matchesSequence(T_OPEN_EXPRESSION, T_ELSE) || c.matchesSequence(TemplateToken.T_ELSE_IF));
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
