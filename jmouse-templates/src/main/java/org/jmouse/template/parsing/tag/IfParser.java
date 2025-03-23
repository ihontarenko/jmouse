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
import org.jmouse.template.node.IfCondition;
import org.jmouse.template.node.IfNode;
import org.jmouse.template.parsing.TemplateParser;

import static org.jmouse.template.TemplateToken.*;

public class IfParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        // Consume the "if" tag.
        cursor.ensure(T_IF);
        IfNode ifNode = new IfNode();
        TemplateParser templateParser = (TemplateParser) context.getParser(TemplateParser.class);
        Parser expressionParser = context.getParser(ExpressionParser.class);

        // Parse the if condition.
        ExpressionNode expressionNode = (ExpressionNode) expressionParser.parse(cursor, context);
        cursor.ensure(T_CLOSE_EXPRESSION);

        // Parse the if block body.
        Node ifBody = new BodyNode();
        templateParser.parse(cursor, ifBody, context, stopper());
        ifNode.addCondition(new IfCondition(expressionNode, (RenderableNode) ifBody.first()));

        // Parse any consecutive else-if branches.
        while (CursorMatcher.sequence(T_ELSE_IF).matches(cursor)) {
            cursor.next(); // Consume the T_OPEN_EXPRESSION.
            cursor.ensure(T_ELSE_IF);
            ExpressionNode elseIfCondition = (ExpressionNode) expressionParser.parse(cursor, context);
            cursor.ensure(T_CLOSE_EXPRESSION);
            Node elseIfBody = new BodyNode();
            templateParser.parse(cursor, elseIfBody, context, stopper());
            ifNode.addCondition(new IfCondition(elseIfCondition, (RenderableNode) elseIfBody.first()));
        }

        // Parse the optional else branch.
        if (CursorMatcher.sequence(T_ELSE).matches(cursor)) {
            cursor.ensure(T_ELSE);
            cursor.ensure(T_CLOSE_EXPRESSION);
            Node elseBody = new BodyNode();
            templateParser.parse(cursor, elseBody, context, stopper());
            ifNode.addElse(new IfCondition(null, (RenderableNode) elseBody.first()));
        }

        // Consume the "end if" tag.
        if (CursorMatcher.sequence(T_END_IF).matches(cursor)) {
            cursor.ensure(T_END_IF);
        } else {
            throw new RuntimeException("Missing end-if tag");
        }

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
