package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.PermissionDeclarationNode;
import org.jmouse.access.el.node.PermissionValueNode;
import org.jmouse.access.el.node.SourceSpanNode;
import org.jmouse.access.el.PolicyParseException;
import org.jmouse.access.policy.model.PolicyPermissionRedirect;
import org.jmouse.access.spi.PermissionRelations.Quantifier;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses one line of a {@code permissions} block: {@code form:read "Read forms"}.
 *
 * <p>Optionally followed by a {@code through} clause saying the permission is asked about a related row
 * rather than about the one named — {@code field:write "…" through each form}. See
 * {@link org.jmouse.access.policy.model.PolicyPermissionRedirect}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.PERMISSION_DECLARATION)
public class PermissionDeclarationParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        PermissionDeclarationNode node   = new PermissionDeclarationNode();
        PermissionValueParser     parser = (PermissionValueParser) context.getParser(PermissionValueParser.class);

        node.setSpan(SourceReader.span(cursor));
        node.setName(((PermissionValueNode) parser.parse(cursor, context)).getPermission());
        node.setDescription(SourceReader.literal(cursor.ensure(T_STRING)));

        if (cursor.isCurrent(AccessToken.T_THROUGH)) {
            node.setThrough(parseRedirect(cursor, node));
        }

        parent.add(node);
    }

    /**
     * {@code through (any|each) <resource>}.
     *
     * <p>⚠️ <strong>The quantifier is required and the failure says why.</strong> Accepting a bare
     * {@code through form} would mean picking one of the two readings on the writer's behalf, and the two
     * are far apart: under {@code any} a field standing on forty-five forms is renamed by whoever owns
     * one of them.
     *
     * <p>⚠️ <strong>The resource is taken as a word and checked later.</strong> Stage 1 knows no classes,
     * so validating it here is impossible — stage 2 resolves it against the resource vocabulary and fails
     * the boot with the names that would have worked.
     */
    private PolicyPermissionRedirect parseRedirect(TokenCursor cursor, PermissionDeclarationNode node) {
        cursor.ensure(AccessToken.T_THROUGH);

        Quantifier quantifier = quantifierFrom(cursor, node);
        String     resource   = cursor.ensure(T_IDENTIFIER).value();

        return new PolicyPermissionRedirect(resource, quantifier);
    }

    private Quantifier quantifierFrom(TokenCursor cursor, PermissionDeclarationNode node) {
        if (cursor.consumeIf(AccessToken.T_ANY)) {
            return Quantifier.ANY;
        }

        if (cursor.consumeIf(AccessToken.T_EACH)) {
            return Quantifier.EACH;
        }

        throw new PolicyParseException(SourceSpanNode.at(node),
                "'through' needs a quantifier — 'through any <resource>' if one related row allowing it "
                + "is enough, or 'through each <resource>' if every one of them must. It is written out "
                + "rather than defaulted because the two are far apart: under 'any', anybody who owns "
                + "one of the forms a field stands on may rename that field for everybody.");
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.permissionDeclaration().matches(cursor);
    }

}
