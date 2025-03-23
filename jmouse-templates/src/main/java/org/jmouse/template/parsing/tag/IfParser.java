package org.jmouse.template.parsing.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.RenderableNode;
import org.jmouse.el.parser.*;
import org.jmouse.template.TemplateToken;
import org.jmouse.template.node.BodyNode;
import org.jmouse.template.node.IfNode;
import org.jmouse.template.parsing.TemplateParser;

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

        Node           container      = BasicNode.forToken(null);
        TemplateParser templateParser = (TemplateParser) context.getParser(TemplateParser.class);
        templateParser.parse(cursor, container, context, stopper());

        RenderableNode renderableNode = (RenderableNode) container.first();

        if (CursorMatcher.sequence(T_ELSE).matches(cursor)) {
            cursor.ensure(TemplateToken.T_ELSE);
            cursor.ensure(TemplateToken.T_CLOSE_EXPRESSION);
            Node           elseContainer      = BasicNode.forToken(null);
            templateParser.parse(cursor, elseContainer, context, stopper());
            RenderableNode elseRenderableNode = (RenderableNode) elseContainer.first();
            System.out.println(elseRenderableNode);
        }

        System.out.println("qwerqweqwe");

        return ifNode;
    }

    public static Matcher<TokenCursor> stopper() {
        Matcher<TokenCursor> matcher  = Matcher.constant(false);

        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_ELSE));
        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_ELSE_IF));
        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_IF));

        return matcher;
    }

    @Override
    public String getName() {
        return "if";
    }

}
