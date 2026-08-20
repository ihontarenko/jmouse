package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.PolicyParseException;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.ScopeDeclarationNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.lexer.BasicToken.*;

/**
 * Parses one line of a {@code scopes} block: {@code @SPACE place parameter=spaceId inside=@ORGANIZATION}.
 *
 * <p>The nature is a bare word the parser does not interpret, and every attribute is optional.</p>
 *
 * <h3>⚠️ Where a scope sits is said here, not by position</h3>
 *
 * <p>Declaration order used to be the width — each scope wrapping the next — which cannot describe two
 * kinds of place that wrap nothing of each other's. {@code inside=@X} says it instead, and
 * {@code beside=@X} is the same statement written from the other side, for the case where saying nothing
 * would leave a reader guessing whether siblings were meant or forgotten.</p>
 */
@Priority(ParserPriority.SCOPE_DECLARATION)
public class ScopeDeclarationParser extends AbstractParser {

    private static final String PARAMETER = "parameter";
    private static final String INSIDE    = "inside";
    private static final String BESIDE    = "beside";
    private static final String REQUIRES  = "requires";

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ScopeDeclarationNode node = new ScopeDeclarationNode();

        node.setSpan(SourceReader.span(cursor));

        cursor.ensure(T_AT);
        node.setName(cursor.ensure(T_IDENTIFIER).value());
        node.setNature(SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING)));

        while (cursor.isCurrent(T_IDENTIFIER) && cursor.isNext(T_EQ)) {
            readAttribute(cursor, node);
        }

        parent.add(node);
    }

    /**
     * Reads one {@code name=value} attribute onto the declaration.
     *
     * <p>⚠️ Refused by name rather than ignored. An attribute nobody reads is a line somebody wrote
     * expecting an effect, and a policy file whose extra words do nothing is the worst kind of
     * documentation.</p>
     */
    private void readAttribute(TokenCursor cursor, ScopeDeclarationNode node) {
        Token attribute = cursor.ensure(T_IDENTIFIER);
        String name     = attribute.value();

        cursor.ensure(T_EQ);

        switch (name) {
            case PARAMETER -> node.setParameter(SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING)));
            case INSIDE    -> node.setInside(readScopeName(cursor));
            case BESIDE    -> node.setBeside(readScopeName(cursor));
            case REQUIRES  -> node.setRequires(readScopeName(cursor));
            default        -> throw new PolicyParseException(
                    SourceReader.span(cursor, attribute).toSourceSpan(),
                    ("a scope declaration takes no '%s'; it accepts 'parameter=', 'inside=', "
                     + "'beside=' and 'requires='").formatted(name));
        }
    }

    /**
     * Reads {@code @ORGANIZATION} — the {@code @} is required, so a scope name never reads as a bare word.
     */
    private String readScopeName(TokenCursor cursor) {
        cursor.ensure(T_AT);

        return cursor.ensure(T_IDENTIFIER).value();
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.scopeDeclaration().matches(cursor);
    }
}
