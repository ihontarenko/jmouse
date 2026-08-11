package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.EntitlementsNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * Parses {@code entitlements { … }} — who is on what.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.ENTITLEMENTS)
public class EntitlementsParser extends PolicyBlockParser<EntitlementsNode, AccessToken> {

    @Override
    protected EntitlementsNode createNode(TokenCursor cursor, ParserContext context) {
        return new EntitlementsNode();
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_ENTITLEMENTS;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.entitlements().matches(cursor);
    }

}
