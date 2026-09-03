package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.CursorMatcher;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.node.IncludeNode;

import static org.jmouse.el.lexer.BasicToken.T_STRING;
import static org.jmouse.script.el.lexer.ScriptToken.T_INCLUDE;

/**
 * Parses {@code include 'common.jms'} — a path recorded, never followed.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.INCLUDE)
public class IncludeParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        IncludeNode node = new IncludeNode();

        node.setSpan(SourceReader.span(cursor));

        cursor.ensure(T_INCLUDE);
        node.setPath(SourceReader.literal(cursor.ensure(T_STRING)));

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.include().matches(cursor);
    }

}
