package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.ScopesNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * Parses {@code scopes { … }} — the block that states this installation's floors.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.SCOPES)
public class ScopesParser extends PolicyBlockParser<ScopesNode, AccessToken> {

    @Override
    protected ScopesNode createNode(TokenCursor cursor, ParserContext context) {
        return new ScopesNode();
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_SCOPES;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.scopes().matches(cursor);
    }

}
