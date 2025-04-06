package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.core.CursorMatcher;
import org.jmouse.el.core.lexer.Token;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.core.parser.ExpressionParser;
import org.jmouse.el.core.parser.Parser;
import org.jmouse.el.core.parser.ParserContext;
import org.jmouse.el.core.parser.TagParser;
import org.jmouse.el.core.rendering.RenderableNode;
import org.jmouse.el.renderable.node.IfNode;
import org.jmouse.el.renderable.node.sub.ConditionBranch;
import org.jmouse.el.renderable.parsing.TemplateParser;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

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
            parseBranch(cursor, T_ELSE_IF, ifNode, context);
        }

        // Parse the optional else branch.
        if (cursor.isCurrent(T_ELSE)) {
            parseBranch(cursor, T_ELSE, ifNode, context);
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
     * @param tagToken the expected branch tag token (T_IF, T_ELSE_IF or T_ELSE)
     * @param ifNode   the IfNode to which the branch is added
     * @param context  the parser context for retrieving sub-parsers
     */
    private void parseBranch(TokenCursor cursor, Token.Type tagToken, IfNode ifNode, ParserContext context) {
        TemplateParser templateParser   = (TemplateParser) context.getParser(TemplateParser.class);
        Parser         expressionParser = context.getParser(ExpressionParser.class);
        ExpressionNode condition        = null;

        // Consume the branch tag (if, else-if or else).
        cursor.ensure(tagToken);
        // Parse the condition expression.
        if (T_ELSE != tagToken) {
            condition = (ExpressionNode) expressionParser.parse(cursor, context);
        }
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
