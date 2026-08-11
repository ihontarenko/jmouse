package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.CapabilityDeclarationNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.el.lexer.BasicToken.T_COMMA;
import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses one line of a {@code capabilities} block:
 * {@code quota storage-byte "Storage written" per organization, space}.
 *
 * <p>The display name is optional and the {@code per} clause is optional; a capability with no
 * {@code per} may be granted anywhere, which is what a product with one floor wants and what a
 * product with five almost never does.</p>
 *
 * <p>⚠️ The kind is consumed as a keyword but stored as the word that was written. Binding maps it,
 * so a file saying {@code quota} against a code that registered a limit fails by name rather than by
 * behaving like one of the two.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.CAPABILITY_DECLARATION)
public class CapabilityDeclarationParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        CapabilityDeclarationNode node = new CapabilityDeclarationNode();

        node.setSpan(SourceReader.span(cursor));
        node.setKind(cursor.ensure(AccessToken.T_GATE, AccessToken.T_LIMIT, AccessToken.T_QUOTA).value());
        node.setKey(SourceReader.hyphenatedName(cursor));

        if (cursor.isCurrent(T_STRING)) {
            node.setDisplayName(SourceReader.literal(cursor.ensure(T_STRING)));
        }

        if (cursor.consumeIf(AccessToken.T_PER)) {
            node.setScopes(readScopes(cursor));
        }

        parent.add(node);
    }

    /**
     * Reads {@code per organization, space} — the places a grant of this capability may be addressed
     * at.
     *
     * <p>Scope <em>names</em>, resolved later against the catalogue. A second enum of grant subjects
     * would be a second addressing scheme for a question places already answer.</p>
     */
    private List<String> readScopes(TokenCursor cursor) {
        List<String> scopes = new ArrayList<>();

        scopes.add(SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING)));

        while (cursor.consumeIf(T_COMMA)) {
            scopes.add(SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING)));
        }

        return scopes;
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.capabilityDeclaration().matches(cursor);
    }

}
