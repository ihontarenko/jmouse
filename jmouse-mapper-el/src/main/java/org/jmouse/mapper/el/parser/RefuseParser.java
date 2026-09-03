package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.CursorLookahead;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.AssertionNode;
import org.jmouse.mapper.el.node.RefuseNode;

/**
 * Reads {@code refuse source|target before|after { condition : "message" … }}.
 *
 * <p>⚠️ {@code refuse source after} is refused here, when the file is read, rather than accepted and
 * quietly never mattering. A mapping does not modify its source, so the assertion would be the same
 * test performed later, having built and filled a target only to throw it away. The grid of four allows
 * it; the language does not, and saying so at load is the difference between a typo and a no-op nobody
 * notices.</p>
 *
 * <h2>⚠️ Its body is read here, not by {@code StatementsParser} — and that is the one place jMM does
 * have an ambiguous position</h2>
 *
 * <p>An assertion reads {@code condition : "message"}; a rule reads {@code property : value}. Both are
 * a name, a colon and something, so offered to global dispatch they compete for every line — and
 * {@link org.jmouse.el.parser.Parser#supports(TokenCursor)} is handed a cursor and nothing else, so
 * neither can ask which block it is standing in.</p>
 *
 * <p>{@code ContextScope} looks like the answer and is not: it holds one value for the current thread,
 * so it answers "who is my immediate parent" rather than "am I inside a refusal", and {@code supports}
 * cannot reach it anyway. So an assertion is treated as what it is — a <strong>line form local to this
 * block</strong>, like a check's arguments in {@code .jmv} — and never enters dispatch. Everything else
 * in the language does.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.REFUSE)
public class RefuseParser extends JmmBlockParser<RefuseNode, JmmToken> {

    @Override
    protected RefuseNode createNode(TokenCursor cursor, ParserContext context) {
        RefuseNode node = new RefuseNode();

        node.setSubject(readSubject(cursor));
        node.setPhase(readPhase(cursor));

        if (node.getSubject() == RefuseNode.Subject.SOURCE && node.getPhase() == RefuseNode.Phase.AFTER) {
            throw new JmmSyntaxException(cursor, "'refuse source after' asserts about an object the "
                    + "mapping never changed, once a target has been built and filled for nothing. "
                    + "Write it as 'refuse source before'");
        }

        return node;
    }

    @Override
    protected JmmToken token() {
        return JmmToken.T_REFUSE;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return opensBlock(cursor);
    }

    /**
     * Reads the assertions, brace to brace.
     *
     * @param cursor the cursor, positioned on the opening brace
     * @param node   the block being filled
     */
    @Override
    protected void parseBody(TokenCursor cursor, RefuseNode node, ParserContext context) {
        cursor.ensure(BasicToken.T_OPEN_CURLY);
        Separators.skip(cursor);

        while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            AssertionNode assertion = readAssertion(cursor);

            node.add(assertion);
            node.addExpression(assertion);
            Separators.skip(cursor);
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);
    }

    /**
     * Whether the cursor is on a refusal block rather than on a property called {@code refuse}.
     *
     * <h2>⚠️ Every reserved word here is a plausible property name</h2>
     *
     * <p>A target with a property called {@code refuse} is ordinary, and a grammar that could not write
     * it down would leave renaming the field as the only advice. So the keyword is only a keyword when
     * a subject follows it — and the test is total rather than merely likely, because a rule's property
     * name is always followed by a colon, so {@code refuse : …} can never be mistaken for
     * {@code refuse source …}.</p>
     *
     * @param cursor the cursor to inspect
     * @return {@code true} when a refusal block starts here
     */
    public static boolean opensBlock(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_REFUSE)
                && cursor.isNext(JmmToken.T_SOURCE, JmmToken.T_TARGET);
    }

    /**
     * Reads whether the block is about the source or the target.
     *
     * @param cursor the cursor, positioned on the subject
     * @return the subject
     */
    private static RefuseNode.Subject readSubject(TokenCursor cursor) {
        if (cursor.consumeIf(JmmToken.T_SOURCE)) {
            return RefuseNode.Subject.SOURCE;
        }

        if (cursor.consumeIf(JmmToken.T_TARGET)) {
            return RefuseNode.Subject.TARGET;
        }

        throw new JmmSyntaxException(cursor, "'refuse' needs a subject — 'source' or 'target'");
    }

    /**
     * Reads when the block runs.
     *
     * @param cursor the cursor, positioned on the phase
     * @return the phase
     */
    private static RefuseNode.Phase readPhase(TokenCursor cursor) {
        if (cursor.consumeIf(JmmToken.T_BEFORE)) {
            return RefuseNode.Phase.BEFORE;
        }

        if (cursor.consumeIf(JmmToken.T_AFTER)) {
            return RefuseNode.Phase.AFTER;
        }

        throw new JmmSyntaxException(cursor, "'refuse' needs a phase — 'before' or 'after'");
    }

    /**
     * Reads one {@code condition : "message"} line.
     *
     * <p>⚠️ The colon still separates a left side from a right side; what stands on each is decided by
     * the block. Here the left is what makes the mapping stop and the right is what to say about it,
     * which is why the block is called {@code refuse} — the line then reads the way it runs, with no
     * negation to write and none to hold in the reader's head.</p>
     *
     * @param cursor the cursor, positioned on the first token of the condition
     * @return the assertion
     */
    private static AssertionNode readAssertion(TokenCursor cursor) {
        AssertionNode assertion = new AssertionNode();
        Token         first     = cursor.current();

        if (first == null) {
            throw new JmmSyntaxException(cursor, "a refusal needs a condition");
        }

        // ⚠️ Stamped before the line is consumed. A condition that will not compile is refused by the
        // binder, long after any cursor exists, and without this it could name no line at all.
        assertion.setSpan(JmmSpans.at(cursor, first));

        int separator = separatorOffset(cursor);
        Token last    = cursor.lookAt(separator - 1);

        assertion.setCondition(SourceReading.text(cursor, first, last));

        cursor.shift(separator);
        cursor.ensure(BasicToken.T_COLON);
        assertion.setMessage(SourceReading.literal(cursor.ensure(BasicToken.T_STRING)));

        return assertion;
    }

    /**
     * Finds the colon that separates this line's condition from its message.
     *
     * <h2>⚠️ It is not the first colon, and that is not a detail</h2>
     *
     * <p>A condition is an expression, and an expression may contain a colon of its own — a ternary is
     * {@code a ? b : c}. Splitting on the first one would cut a condition in half and hand the tail to
     * the message, where it would be a string nobody wrote rather than an error.</p>
     *
     * <p>So the line is anchored from its end instead: the message is the trailing string literal, and
     * the separator is the colon immediately before it. Nothing else can occupy that position, because
     * a message is required and is always the last thing on the line.</p>
     *
     * @param cursor the cursor, positioned on the first token of the condition
     * @return the lookahead offset of the separating colon
     */
    private static int separatorOffset(TokenCursor cursor) {
        int separator = -1;

        // ⚠️ The scan asks only whether a token is still there. Testing it against a set of types
        // would stop at the first operator, and a condition is made of operators.
        for (int offset = 0; cursor.lookAt(offset) != null; offset++) {
            if (isLineEnd(cursor, offset)) {
                break;
            }

            if (CursorLookahead.at(cursor, offset, BasicToken.T_COLON)
                    && CursorLookahead.at(cursor, offset + 1, BasicToken.T_STRING)) {
                separator = offset;
            }
        }

        if (separator < 1) {
            throw new JmmSyntaxException(cursor,
                    "a refusal reads 'condition : \"message\"' — the message is required, and says what "
                            + "is wrong with the data");
        }

        return separator;
    }

    /**
     * Whether the lookahead offset has run past the end of this line.
     *
     * @param cursor the cursor to read from
     * @param offset the lookahead offset
     * @return {@code true} at a newline, a semicolon or the block's closing brace
     */
    private static boolean isLineEnd(TokenCursor cursor, int offset) {
        return CursorLookahead.at(cursor, offset,
                BasicToken.T_NEW_LINE, BasicToken.T_EOL, BasicToken.T_SEMICOLON, BasicToken.T_CLOSE_CURLY);
    }
}
