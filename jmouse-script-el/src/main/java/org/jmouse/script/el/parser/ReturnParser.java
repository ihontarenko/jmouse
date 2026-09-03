package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.CursorMatcher;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.lexer.ScriptToken;
import org.jmouse.script.el.node.ReturnNode;

/**
 * Parses {@code return}, with or without a value.
 *
 * <h2>⚠️ Where a bare {@code return} ends</h2>
 *
 * <p>Nothing but the end of the line separates {@code return} from what may follow it, so the value is
 * read only when something on <em>this</em> line could be one. Reading greedily instead would make
 * {@code return} followed by {@code end} on the next line swallow the {@code end} — and the body would
 * then close on whatever came after, which is a body silently a statement longer than it was written.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.RETURN)
public class ReturnParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ReturnNode node = new ReturnNode();

        node.setSpan(SourceReader.span(cursor));

        cursor.ensure(ScriptToken.T_RETURN);

        if (carriesValue(cursor)) {
            node.setValue(Expressions.read(cursor, context));
        }

        parent.add(node);
    }

    /**
     * Whether a value was written after the keyword.
     *
     * @param cursor the cursor, positioned immediately after {@code return}
     * @return {@code true} when what follows on this line is an expression
     */
    private boolean carriesValue(TokenCursor cursor) {
        return cursor.hasNext() && !cursor.isCurrent(
                BasicToken.T_NEW_LINE, BasicToken.T_EOL, BasicToken.T_SEMICOLON, BasicToken.T_HASH,
                ScriptToken.T_END, LanguageToken.T_ELSE, LanguageToken.T_ELSE_IF
        );
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.returned().matches(cursor);
    }

}
