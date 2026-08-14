package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.PolicyParseException;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.RoleNode;
import org.jmouse.access.el.node.SingleScopeNode;
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
        RoleNode node = new RoleNode(SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING)));

        readAssignableAt(cursor, context, node);

        return node;
    }

    /**
     * Reads the optional {@code assignable @SCOPE} between the name and the body.
     *
     * <p>⚠️ <strong>The scope is read by {@link SingleScopeParser}, not by matching an identifier</strong>
     * — so {@code assignable @SPACE:kyiv} is refused here rather than accepted and quietly ignored.
     * Naming an instance is meaningless in this position: "may be handed out at a workspace" is a
     * statement about a <em>kind</em> of place, and saying which workspace would make the role
     * un-assignable anywhere else while looking like it merely gave an example.
     *
     * <p>Optional, because a document that never becomes rows has no use for it.
     */
    private void readAssignableAt(TokenCursor cursor, ParserContext context, RoleNode node) {
        if (!cursor.consumeIf(AccessToken.T_ASSIGNABLE)) {
            return;
        }

        SingleScopeParser scopes = (SingleScopeParser) context.getParser(SingleScopeParser.class);
        SingleScopeNode   scope  = (SingleScopeNode) scopes.parse(cursor, context);

        if (scope.namesAnInstance()) {
            throw new PolicyParseException(
                    SourceReader.span(cursor).toSourceSpan(),
                    ("'assignable' takes a kind of place and not one place: write 'assignable @%s'. "
                     + "Naming an instance would make the role un-assignable everywhere else, while "
                     + "reading as though it were an example").formatted(scope.getKind()));
        }

        node.setAssignableAt(scope.getKind());
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
