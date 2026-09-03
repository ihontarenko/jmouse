package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.language.node.IfBranchNode;
import org.jmouse.el.language.node.IfNode;
import org.jmouse.el.language.parser.AbstractBranchParser;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.CursorMatcher;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.lexer.ScriptToken;

import static org.jmouse.el.language.lexer.LanguageToken.*;

/**
 * Parses {@code if … then … elseif … then … else … end}.
 *
 * <h2>⚠️ The engine's branch, wearing this dialect's delimiters</h2>
 *
 * <p>{@link IfNode} and {@link IfBranchNode} are the engine's, and so is the loop that reads a chain of
 * branches. What jMS supplies is three lines of difference: {@code then} opens a branch body,
 * {@code end} closes the chain, and the bodies are word-delimited.</p>
 *
 * <p>The alternative — a second {@code if} with its own node, its own evaluation and its own idea of
 * what a branch is — is how two conditionals end up disagreeing about a falsy value in a language that
 * has exactly one.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.BRANCH)
public class BranchParser extends AbstractBranchParser<IfNode, IfBranchNode, LanguageToken> {

    @Override
    protected IfNode createNode(TokenCursor cursor, ParserContext context) {
        return new IfNode();
    }

    @Override
    protected LanguageToken token() {
        return T_IF;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.branch().matches(cursor);
    }

    @Override
    protected IfBranchNode createBranch(TokenCursor cursor, IfNode node, ParserContext context) {
        return new IfBranchNode();
    }

    /**
     * Reads a branch's condition, where it has one.
     *
     * <p>An {@code else} carries none — the engine reads a null condition as "always" — so the whole
     * header is one {@code if}: what opened this branch was consumed a moment ago, and it is either a
     * word that takes a condition or the one that does not.</p>
     */
    @Override
    protected void parseBranchHeader(TokenCursor cursor, IfBranchNode branch, ParserContext context) {
        if (cursor.isPrevious(T_IF, T_ELSE_IF)) {
            branch.setCondition(Expressions.read(cursor, context));
            cursor.ensure(ScriptToken.T_THEN);
        }
    }

    @Override
    protected boolean hasNextBranch(TokenCursor cursor) {
        return cursor.consumeIf(T_ELSE_IF, T_ELSE);
    }

    @Override
    protected void addBranch(IfNode node, IfBranchNode branch) {
        node.addBranch(branch);
    }

    @Override
    protected Class<? extends Parser> statementsParser() {
        return ScriptBodyParser.class;
    }

    @Override
    protected void closeBlock(TokenCursor cursor) {
        cursor.ensure(ScriptToken.T_END);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        return (S) SourceReader.span(cursor);
    }

}
