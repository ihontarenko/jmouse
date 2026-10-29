package org.jmouse.el.grammar;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

/**
 * Reads one line of a block — a pin, a pad, a part — into its node. A line is what it is because of the block it stands
 * in, never because of how it begins, so it is never offered to the engine's dispatch.
 *
 * @param <L> the line's node
 */
public abstract class LineParser<L extends Expression> extends AbstractParser {

    /** Reads the line the cursor stands on; the cursor is left at its end. */
    protected abstract L line(TokenCursor cursor, ParserContext context);

    @Override
    public final void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(line(cursor, context));
    }

    @Override
    public final boolean supports(TokenCursor cursor) {
        return false;
    }
}
