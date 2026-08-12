package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.ActionsNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * Parses {@code actions { … }} — the block that states what this installation's calls do.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.ACTIONS)
public class ActionsParser extends PolicyBlockParser<ActionsNode, AccessToken> {

    @Override
    protected ActionsNode createNode(TokenCursor cursor, ParserContext context) {
        return new ActionsNode();
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_ACTIONS;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.actions().matches(cursor);
    }

}
