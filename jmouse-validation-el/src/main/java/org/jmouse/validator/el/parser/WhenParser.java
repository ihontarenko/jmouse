package org.jmouse.validator.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.language.parser.AbstractBranchParser;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.validator.el.lexer.JmvToken;
import org.jmouse.validator.el.node.WhenBranchNode;
import org.jmouse.validator.el.node.WhenNode;

/**
 * Reads {@code when <condition> { … } [otherwise { … }]}.
 *
 * <p>⚠️ It is {@link AbstractBranchParser} rather than anything of its own, because {@code when} and
 * {@code otherwise} are {@code if} and {@code else} with different words on them. The framework
 * already reads a branch's header, its body, and whether another branch follows; a hand-written
 * version of that is the same code with this language's nouns in it and one fewer place fixed when the
 * framework's is.</p>
 *
 * <p>⚠️ There are at most two branches, and only the first is guarded. {@code otherwise} takes no
 * condition, so {@link #hasNextBranch} consuming it once is what stops a third.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmvParserPriority.WHEN)
public class WhenParser extends AbstractBranchParser<WhenNode, WhenBranchNode, JmvToken> {

    @Override
    protected WhenNode createNode(TokenCursor cursor, ParserContext context) {
        return new WhenNode();
    }

    @Override
    protected JmvToken token() {
        return JmvToken.T_WHEN;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.opensBlock(cursor, JmvToken.T_WHEN);
    }

    @Override
    protected WhenBranchNode createBranch(TokenCursor cursor, WhenNode node, ParserContext context) {
        return new WhenBranchNode();
    }

    /**
     * Reads the guard, but only on the branch that has one.
     *
     * <p>The base class has already consumed {@code when} or {@code otherwise}, so which branch this is
     * is a question about the token behind the cursor.</p>
     */
    @Override
    protected void parseBranchHeader(TokenCursor cursor, WhenBranchNode branch, ParserContext context) {
        if (cursor.isPrevious(JmvToken.T_WHEN)) {
            branch.setCondition(ExpressionSlice.toBlock(cursor));
        }
    }

    @Override
    protected boolean hasNextBranch(TokenCursor cursor) {
        return cursor.consumeIf(JmvToken.T_OTHERWISE);
    }

    @Override
    protected void addBranch(WhenNode node, WhenBranchNode branch) {
        node.addBranch(branch);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        Token token = cursor.current();

        return (S) SpanNode.of(token.lineNumber(), token.offset());
    }
}
