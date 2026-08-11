package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.PlanGrantNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_INT;
import static org.jmouse.el.lexer.BasicToken.T_NUMERIC;
import static org.jmouse.el.lexer.BasicToken.T_LONG;

/**
 * Parses one line of a plan's body: {@code storage-byte 100GB per month}, {@code seat unlimited}, or
 * a bare {@code custody}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.PLAN_GRANT)
public class PlanGrantParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        PlanGrantNode node = new PlanGrantNode();

        node.setSpan(SourceReader.span(cursor));
        node.setCapability(SourceReader.hyphenatedName(cursor));

        if (cursor.consumeIf(AccessToken.T_UNLIMITED)) {
            node.setUnlimited(true);
        } else if (cursor.isCurrent(T_NUMERIC, T_INT, T_LONG)) {
            node.setQuantity(readQuantity(cursor));
        }

        if (cursor.consumeIf(AccessToken.T_PER)) {
            node.setPeriod(SourceReader.literal(cursor.ensure(T_IDENTIFIER, BasicToken.T_STRING)));
        }

        parent.add(node);
    }

    /**
     * Reads an amount, rejoining a unit suffix the lexer split off.
     *
     * <p>⚠️ {@code 100GB} reaches here as {@code 100} and {@code GB} — the expression language's
     * splitter breaks a word at the digit boundary and nothing in this language can ask it not to. The
     * two are rejoined into the string the file wrote, and what {@code GB} <em>means</em> stays the
     * product's business.
     *
     * <p>The suffix cannot swallow the next line: a body line is {@code capability amount}, so the
     * token after an amount is either {@code per}, a newline, or the closing brace. Only an identifier
     * sitting on the same line can be a unit, and {@code per} is excluded by name because it is the
     * one identifier that legitimately follows an amount.
     */
    private String readQuantity(TokenCursor cursor) {
        String amount = cursor.ensure(T_NUMERIC, T_INT, T_LONG).value();

        if (cursor.isCurrent(T_IDENTIFIER) && !cursor.isCurrent(AccessToken.T_PER)) {
            return amount + cursor.ensure(T_IDENTIFIER).value();
        }

        return amount;
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.planGrant().matches(cursor);
    }

}
