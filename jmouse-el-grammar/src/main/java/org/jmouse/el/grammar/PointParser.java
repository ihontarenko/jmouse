package org.jmouse.el.grammar;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

/** {@code x, y}: two quantities and the comma between them, ending where the caller's cursor ends. */
public class PointParser extends AbstractParser {

    /** The x of a point ends at its comma. */
    private static final BoundedTokenCursor.Boundary COMMA = (token, next, depth) -> depth == 0 && token.type() == BasicToken.T_COMMA;

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        QuantityLiteralNode x = QuantityValueParser.read(new BoundedTokenCursor(cursor, COMMA), context);
        if (!cursor.isCurrent(BasicToken.T_COMMA)) {
            throw GrammarRefusal.here(cursor, "a point is 'x, y'");
        }
        cursor.next();
        parent.add(new PointNode(x, QuantityValueParser.read(cursor, context)));
    }

    /** One point from the cursor, ending where the boundary says. */
    public static PointNode read(TokenCursor cursor, ParserContext context) {
        return (PointNode) context.getParser(PointParser.class).parse(cursor, context);
    }
}
