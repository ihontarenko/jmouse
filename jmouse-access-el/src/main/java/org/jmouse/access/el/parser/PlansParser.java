package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.PlansNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * Parses {@code plans { … }} — the block holding every bundle.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.PLANS)
public class PlansParser extends PolicyBlockParser<PlansNode, AccessToken> {

    @Override
    protected PlansNode createNode(TokenCursor cursor, ParserContext context) {
        return new PlansNode();
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_PLANS;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.plans().matches(cursor);
    }

}
