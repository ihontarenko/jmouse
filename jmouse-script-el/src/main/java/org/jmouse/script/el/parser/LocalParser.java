package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.CursorMatcher;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.lexer.ScriptToken;
import org.jmouse.script.el.node.LocalNode;

/**
 * Parses {@code local slot = @store.next_slot(entry)}.
 *
 * <p>The {@code local} in front is what makes the {@code =} unambiguous here — the same trick
 * {@code .jmm}'s {@code let} uses, and the reason an assignment needs a check that this does not.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.LOCAL)
public class LocalParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        LocalNode node = new LocalNode();

        node.setSpan(SourceReader.span(cursor));

        cursor.ensure(ScriptToken.T_LOCAL);
        node.setName(SourceReader.name(cursor));
        cursor.ensure(BasicToken.T_EQ);
        node.setValue(Expressions.read(cursor, context));

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.local().matches(cursor);
    }

}
