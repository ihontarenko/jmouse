package org.jmouse.access.el.parser;

import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.IncludeNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.access.el.lexer.AccessToken.T_INCLUDE;
import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses {@code include 'bootstrap.jmp'} — a path recorded, never followed.
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
        return cursor.matchesSequence(T_INCLUDE, T_STRING);
    }

}
