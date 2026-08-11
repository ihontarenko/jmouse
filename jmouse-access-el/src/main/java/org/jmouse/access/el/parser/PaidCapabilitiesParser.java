package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.PaidCapabilitiesNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.el.lexer.BasicToken.T_COMMA;

/**
 * Parses {@code paid custody, parametric-search} — the capabilities closed until something grants
 * them.
 *
 * <p>Repeatable, so a product selling ten things writes ten readable lines rather than one long one.
 * {@link org.jmouse.access.el.node.CapabilitiesNode} joins them to the declarations.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.PAID_CAPABILITIES)
public class PaidCapabilitiesParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        PaidCapabilitiesNode node = new PaidCapabilitiesNode();

        node.setSpan(SourceReader.span(cursor));

        cursor.ensure(AccessToken.T_PAID);

        List<String> keys = new ArrayList<>();

        keys.add(SourceReader.hyphenatedName(cursor));

        while (cursor.consumeIf(T_COMMA)) {
            keys.add(SourceReader.hyphenatedName(cursor));
        }

        node.setKeys(keys);
        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.paidCapabilities().matches(cursor);
    }

}
