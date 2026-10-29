package org.jmouse.el.grammar;


import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.QuantityParser;
import org.jmouse.units.Quantity;

/**
 * A value that must be a number with at most its unit — a coordinate, a size, a range bound —, read by the engine's
 * {@link QuantityParser} into its {@link QuantityLiteralNode}. Where the value ends is the caller's to say, by the
 * engine's {@code BoundedTokenCursor} it hands in: everything up to there must be the one quantity.
 */
public class QuantityValueParser extends AbstractParser {

    /**
     * The engine's quantity parser, held here rather than registered: offered to the engine's dispatch, it would take a
     * number in a jMS body read through the same context.
     */
    private final QuantityParser quantities = new QuantityParser();

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Token first = cursor.current();
        QuantityLiteralNode quantity = quantities.read(cursor, context)
                .filter(read -> !cursor.hasNext())
                .orElseThrow(() -> GrammarRefusal.at(cursor, first, "a number with an optional unit is expected here"));
        // Kept as its author wrote it — 4.7 kΩ, 1 µF —, not as the lexer read it, so it is written back that way.
        Quantity read = quantity.getQuantity();
        String written = AuthoredText.of(cursor).written(first, cursor.lookAt(-1));
        parent.add(new QuantityLiteralNode(new Quantity(read.value(), read.unit(), written)));
    }

    /** One quantity from the cursor, ending where the boundary says; the cursor is left there. */
    public static QuantityLiteralNode read(TokenCursor cursor, ParserContext context) {
        return (QuantityLiteralNode) context.getParser(QuantityValueParser.class).parse(cursor, context);
    }
}
