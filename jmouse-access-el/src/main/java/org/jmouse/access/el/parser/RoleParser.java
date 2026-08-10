package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.RoleNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses {@code role SPACE_ADMIN { … }}.
 *
 * <p>The span is not set here on purpose: the block parser captures one before consuming
 * {@code role}, so it points at the keyword rather than at the brace that happens to follow the
 * name.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.ROLE)
public class RoleParser extends PolicyBlockParser<RoleNode, AccessToken> {

    @Override
    protected RoleNode createNode(TokenCursor cursor, ParserContext context) {
        return new RoleNode(SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING)));
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_ROLE;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.role().matches(cursor);
    }

}
