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
 * Parses one line of a {@code scopes} block: {@code @SPACE place parameter=spaceId}.
 *
 * <p>The nature is a bare word the parser does not interpret, and {@code parameter=} is optional —
 * only a place carries one. Position in the block is the scope's width, so nothing here reads or
 * writes a rank.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.SCOPE_DECLARATION)
public class ScopeDeclarationParser extends AbstractParser {

    /** The only attribute a scope declaration accepts, and the reason it is not a bare third word. */
    private static final String PARAMETER = "parameter";

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ScopeDeclarationNode node = new ScopeDeclarationNode();

        node.setSpan(SourceReader.span(cursor));

        cursor.ensure(T_AT);

        node.setName(cursor.ensure(T_IDENTIFIER).value());
        node.setNature(SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING)));

        if (cursor.isCurrent(T_IDENTIFIER) && cursor.isNext(T_EQ)) {
            node.setParameter(readParameter(cursor));
        }

        parent.add(node);
    }

    /**
     * Reads {@code parameter=spaceId}, and refuses any other attribute by name.
     *
     * <p>⚠️ Skipping an attribute it does not recognise would leave its tokens for whichever parser
     * is offered them next, and the failure would surface a line away from the typo that caused it —
     * if it surfaced at all. A scope accepts one attribute, and says so.</p>
     *
     * @param cursor the cursor, positioned on the attribute's name
     * @return the parameter's value
     * @throws PolicyParseException when the attribute is not {@code parameter}
     */
    private String readParameter(TokenCursor cursor) {
        Token attribute = cursor.ensure(T_IDENTIFIER);

        if (!PARAMETER.equals(attribute.value())) {
            throw new PolicyParseException(
                    SourceReader.span(cursor, attribute).toSourceSpan(),
                    "a scope declaration takes no '%s'; the only attribute it accepts is 'parameter='"
                            .formatted(attribute.value())
            );
        }

        cursor.ensure(T_EQ);

        return SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING));
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.scopeDeclaration().matches(cursor);
    }

}
