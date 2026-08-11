package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.CapabilitiesNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * Parses {@code capabilities { … }} — the block that states what a grant in this installation can be
 * about.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.CAPABILITIES)
public class CapabilitiesParser extends PolicyBlockParser<CapabilitiesNode, AccessToken> {

    @Override
    protected CapabilitiesNode createNode(TokenCursor cursor, ParserContext context) {
        return new CapabilitiesNode();
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_CAPABILITIES;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.capabilities().matches(cursor);
    }

}
