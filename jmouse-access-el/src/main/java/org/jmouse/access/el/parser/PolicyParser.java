package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.PolicyNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * Parses {@code policy "name" { … }} — the optional wrapper around a whole file.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.POLICY)
public class PolicyParser extends PolicyBlockParser<PolicyNode, AccessToken> {

    @Override
    protected PolicyNode createNode(TokenCursor cursor, ParserContext context) {
        return new PolicyNode(SourceReader.literal(cursor.ensure(BasicToken.T_STRING)));
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_POLICY;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.policy().matches(cursor);
    }

}
