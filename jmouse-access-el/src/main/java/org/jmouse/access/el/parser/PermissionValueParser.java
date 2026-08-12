package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.PermissionValueNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.lexer.BasicToken.*;

/**
 * Parses a permission: {@code form:read}, or {@code form:*} for a whole namespace.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.PERMISSION_VALUE)
public class PermissionValueParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        PermissionValueNode node = new PermissionValueNode();

        node.setSpan(SourceReader.span(cursor));
        // Any segment may be one of the language's own words — `role:read` is an ordinary permission
        // wherever roles are administered. See AccessToken#nameTokens.
        node.setNamespace(cursor.ensure(AccessToken.nameTokens()).value());

        cursor.ensure(T_COLON);

        node.setAction(action(cursor));

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.permissionValue().matches(cursor);
    }

    /**
     * Everything after the first colon, colons and all.
     *
     * <p>{@code form:write:system} and {@code space:module:restrict} are ordinary permissions in a
     * real installation, so a permission is a namespace and then whatever the product wanted to say —
     * kept as one string, because nothing here or downstream looks inside it.
     *
     * <p>A {@code *} ends it: {@code form:*} names a whole namespace and there is nothing narrower to
     * say after that.
     *
     * @param cursor the cursor to read from
     * @return the action, with any further segments joined back on
     */
    private static String action(TokenCursor cursor) {
        Token         first  = cursor.ensure(AccessToken.nameTokensWithWildcard());
        StringBuilder action = new StringBuilder(first.value());

        if (first.type() == T_MULTIPLY) {
            return action.toString();
        }

        while (cursor.isCurrent(T_COLON)) {
            cursor.ensure(T_COLON);
            action.append(':').append(cursor.ensure(AccessToken.nameTokens()).value());
        }

        return action.toString();
    }

}
