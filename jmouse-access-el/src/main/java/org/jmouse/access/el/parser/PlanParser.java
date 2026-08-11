package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.PolicyParseException;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.PlanNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.lexer.BasicToken.T_INT;
import static org.jmouse.el.lexer.BasicToken.T_NUMERIC;
import static org.jmouse.el.lexer.BasicToken.T_LONG;
import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses one bundle's header: {@code plan business "Business" order 30 extends team note "…"}.
 *
 * <p>Everything after the code is optional and may be written in any order, because three optional
 * clauses in a fixed sequence is a rule nobody remembers and a diff nobody can read. The loop takes
 * whichever keyword comes next and refuses anything else <em>by name</em> — skipping an unrecognised
 * word would leave its tokens for whatever parser is offered them next, and the failure would surface
 * a line away from the typo that caused it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.PLAN)
public class PlanParser extends PolicyBlockParser<PlanNode, AccessToken> {

    @Override
    protected PlanNode createNode(TokenCursor cursor, ParserContext context) {
        PlanNode node = new PlanNode(SourceReader.hyphenatedName(cursor));

        if (cursor.isCurrent(T_STRING)) {
            node.setDisplayName(SourceReader.literal(cursor.ensure(T_STRING)));
        }

        readAttributes(cursor, node);

        return node;
    }

    /**
     * Reads {@code order}, {@code extends} and {@code note} in whatever order they were written.
     *
     * @throws PolicyParseException when a bundle header carries a word that is none of the three
     */
    private void readAttributes(TokenCursor cursor, PlanNode node) {
        while (!cursor.isCurrent(org.jmouse.el.lexer.BasicToken.T_OPEN_CURLY)) {
            if (cursor.consumeIf(AccessToken.T_ORDER)) {
                node.setOrder(Integer.parseInt(cursor.ensure(T_NUMERIC, T_INT, T_LONG).value()));
            } else if (cursor.consumeIf(AccessToken.T_EXTENDS)) {
                node.setExtendsCode(SourceReader.hyphenatedName(cursor));
            } else if (cursor.consumeIf(AccessToken.T_NOTE)) {
                node.setNote(SourceReader.literal(cursor.ensure(T_STRING)));
            } else {
                throw unexpected(cursor);
            }
        }
    }

    private PolicyParseException unexpected(TokenCursor cursor) {
        Token token = cursor.peek();

        return new PolicyParseException(
                SourceReader.span(cursor, token).toSourceSpan(),
                ("a plan header takes 'order', 'extends' and 'note', and '%s' is none of them. "
                 + "Capabilities go in the body — a header that accepted them would make the grammar "
                 + "depend on this product never naming one 'note'").formatted(token.value())
        );
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_PLAN;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.plan().matches(cursor);
    }

}
