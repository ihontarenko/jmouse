package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.SingleScopeNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.PlaceholderParser;

import static org.jmouse.el.lexer.BasicToken.*;

/**
 * Parses a scope reference: {@code @SPACE}, {@code @SPACE:kyiv}, {@code @SPACE:*} or
 * {@code @SPACE:${default.space}}.
 *
 * <p>⚠️ Quote an instance a bare identifier cannot hold — {@code @SPACE:'my-space'}. The lexer reads
 * a bare name as letters, digits and underscores, so a hyphen ends it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.SINGLE_SCOPE)
public class SingleScopeParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        SingleScopeNode node = new SingleScopeNode();

        node.setSpan(SourceReader.span(cursor));

        cursor.ensure(T_AT);
        node.setKind(cursor.ensure(T_IDENTIFIER).value());

        if (cursor.consumeIf(T_COLON)) {
            node.setInstance(readInstance(cursor, context));
        }

        parent.add(node);
    }

    /**
     * Reads the instance after the colon in whichever of its four forms was written.
     *
     * @param cursor  the cursor, positioned on the instance
     * @param context the parser context, for the placeholder parser
     * @return the instance, unquoted, or the placeholder exactly as it was written
     */
    private String readInstance(TokenCursor cursor, ParserContext context) {
        if (cursor.isCurrent(T_DOLLAR)) {
            Expression placeholder = (Expression) context.getParser(PlaceholderParser.class).parse(cursor, context);
            return placeholder.toSource();
        }

        return SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_MULTIPLY, T_STRING));
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.singleScope().matches(cursor);
    }

}
