package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.PermissionsNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * Parses {@code permissions { … }} — the block that states this installation's permissions.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.PERMISSIONS)
public class PermissionsParser extends PolicyBlockParser<PermissionsNode, AccessToken> {

    @Override
    protected PermissionsNode createNode(TokenCursor cursor, ParserContext context) {
        return new PermissionsNode();
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_PERMISSIONS;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.permissions().matches(cursor);
    }

}
