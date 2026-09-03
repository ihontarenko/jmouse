package org.jmouse.validator.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.validator.el.lexer.JmvToken;
import org.jmouse.validator.el.node.InvariantNode;

/**
 * Reads {@code invariant <condition> : <message>}.
 *
 * <p>Not a block parser: an invariant is one statement, so there is no body for
 * {@link org.jmouse.el.language.parser.AbstractBodyParser} to read.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmvParserPriority.INVARIANT)
public class InvariantParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        InvariantNode node = new InvariantNode();

        cursor.ensure(JmvToken.T_INVARIANT);
        node.setCondition(ExpressionSlice.toColon(cursor, "an invariant needs a condition"));

        // ⚠️ Said here rather than left to `ensure`, which would report a missing T_COLON at an offset.
        // Whoever reads that message wrote a sentence about their data and is owed one back.
        if (!cursor.isCurrent(BasicToken.T_COLON)) {
            throw new JmvSyntaxException(cursor,
                    "an invariant needs a message after ':' — it belongs to no field, so without one "
                    + "a reader is left with the word 'invalid' and a record to guess about");
        }

        cursor.ensure(BasicToken.T_COLON);
        node.setMessage(ExpressionSlice.message(cursor));

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.opensBlock(cursor, JmvToken.T_INVARIANT);
    }
}
