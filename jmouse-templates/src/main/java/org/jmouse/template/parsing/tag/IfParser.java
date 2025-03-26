package org.jmouse.template.parsing.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.rendering.RenderableNode;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.template.node.IfNode;
import org.jmouse.template.node.sub.ConditionBranch;
import org.jmouse.template.parsing.TemplateParser;

import static org.jmouse.template.lexer.TemplateToken.*;

/**
 * Parses an "if" tag with support for else-if and else branches.
 * <p>
 * This parser processes the "if" condition, its corresponding block, any consecutive
 * else-if branches, an optional else branch, and finally the end-if tag. All branches
 * are aggregated into an {@link IfNode}.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class IfParser implements TagParser {

    /**
     * Creates a matcher that stops branch parsing when encountering an else, else-if, or end-if tag.
     *
     * @return a Matcher for stopping conditions
     */
    public static Matcher<TokenCursor> stopper() {
        Matcher<TokenCursor> matcher = Matcher.constant(false);

        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_ELSE));
        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_ELSE_IF));
        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_IF));

        return matcher;
    }

    /**
     * Parses an if tag along with its branches.
     * <p>
     * It consumes the "if" tag and its condition, then parses the if branch, any subsequent else-if branches,
     * and an optional else branch before consuming the end-if tag.
     * </p>
     *
     * @param cursor  the token cursor positioned at the if tag
     * @param context the parser context for retrieving sub-parsers
     * @return a RenderableNode representing the complete if structure
     */
    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        IfNode ifNode = new IfNode();

        // Consume the "if" tag.
        parseBranch(cursor, T_IF, ifNode, context);

        // Parse any consecutive else-if branches.
        while (cursor.isCurrent(T_ELSE_IF)) {
            // Consume the "else-if" tag.
            parseBranch(cursor, T_ELSE_IF, ifNode, context);
        }

        // Parse the optional else branch.
        if (cursor.isCurrent(T_ELSE)) {
            cursor.ensure(T_ELSE);
            cursor.ensure(T_CLOSE_EXPRESSION);
            TemplateParser templateParser = (TemplateParser) context.getParser(TemplateParser.class);
            RenderableNode elseBlock      = (RenderableNode) templateParser.parse(cursor, context, stopper());
            ifNode.addBranch(new ConditionBranch(null, elseBlock));
        }

        // Consume the "end if" tag.
        cursor.ensure(T_END_IF);

        return ifNode;
    }

    /**
     * Parses a single branch (if or else-if) and adds it to the provided IfNode.
     * <p>
     * The method consumes the branch tag, parses the condition, ensures the expression is closed,
     * and parses the branch body until a stopper is encountered.
     * </p>
     *
     * @param cursor   the token cursor
     * @param tagToken the expected branch tag token (T_IF or T_ELSE_IF)
     * @param ifNode   the IfNode to which the branch is added
     * @param context  the parser context for retrieving sub-parsers
     */
    private void parseBranch(TokenCursor cursor, Token.Type tagToken, IfNode ifNode, ParserContext context) {
        TemplateParser templateParser   = (TemplateParser) context.getParser(TemplateParser.class);
        Parser         expressionParser = context.getParser(ExpressionParser.class);

        // Consume the branch tag (if or else-if).
        cursor.ensure(tagToken);
        // Parse the condition expression.
        ExpressionNode condition = (ExpressionNode) expressionParser.parse(cursor, context);
        // Ensure closing of the condition expression.
        cursor.ensure(T_CLOSE_EXPRESSION);
        // Parse the branch body until a stopper is reached.
        RenderableNode body = (RenderableNode) templateParser.parse(cursor, context, stopper());
        // Add the branch to the IfNode.
        ifNode.addBranch(new ConditionBranch(condition, body));
    }

    @Override
    public String getName() {
        return "if";
    }

}
