package org.jmouse.sheet.parser;

import org.jmouse.core.Priority;
import org.jmouse.sheet.SheetSyntaxException;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

/**
 * Whatever begins a line in a block and is neither a field nor an entry — a text in quotes, a separator, a brace on
 * its own — refused in words. Asked last.
 *
 * <p>⚠️ Without it the engine's statement reader falls back to the expression parser, which sheet does not register: a
 * stray {@code ,} would fail as a missing parser instead of as a line that does not read.</p>
 */
@Priority(SheetParserPriority.UNREADABLE)
public class UnreadableLineParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (!cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL)) {
            throw new SheetSyntaxException(cursor, "a block is not closed: '}' is missing");
        }
        throw new SheetSyntaxException(cursor, "'" + cursor.lookAt(0).value() + "' does not begin a line: a line "
                + "in a block is a field — name value — or an entry — id { … }");
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return true;
    }
}
