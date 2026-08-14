package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.VariablesNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * Parses {@code variables { … }} — the block that states what is true of every call.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.VARIABLES)
public class VariablesParser extends PolicyBlockParser<VariablesNode, AccessToken> {

    @Override
    protected VariablesNode createNode(TokenCursor cursor, ParserContext context) {
        return new VariablesNode();
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_VARIABLES;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.variables().matches(cursor);
    }

}
