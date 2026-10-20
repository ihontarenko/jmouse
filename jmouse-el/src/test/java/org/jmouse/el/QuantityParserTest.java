package org.jmouse.el;

import org.jmouse.el.extension.CoreExtension;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.QuantityParser;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A number and, at most, its unit, read from the cursor: what is read, and where the cursor is left. */
class QuantityParserTest {

    @Test
    void readsAUnitApartOrJoined() {
        assertRead("100 nF", 100e-9, "F", "<end>");
        assertRead("2.2µs", 2.2e-6, "s", "<end>");
        assertRead("200 ppm/K", 200e-6, "ppm/K", "<end>");
        assertRead("- 5 V", -5, "V", "<end>");
    }

    @Test
    void stopsWhereTheUnitEnds() {
        assertRead("10 kOhm in 1 .. 100", 1e4, "Ohm", "in");
        assertRead("5 V, 6 V", 5, "V", ",");
        assertRead("25 \"description\"", 25, "", "\"description\"");
    }

    @Test
    void aWordThatIsNotAUnitIsLeftForTheLanguage() {
        assertRead("2 pi", 2, "", "pi");
        assertRead("10 step 2", 10, "", "step");
    }

    @Test
    void aWordJoinedToTheNumberIsNoQuantity() {
        assertTrue(read("74hc595").quantity().isEmpty());
        assertTrue(read("2pi").quantity().isEmpty());
    }

    private static void assertRead(String text, double value, String unit, String next) {
        Read read = read(text);
        QuantityLiteralNode node = read.quantity().orElseThrow(() -> new AssertionError(text + " did not read"));
        assertEquals(value, node.getQuantity().value(), Math.abs(value) * 1e-12, text);
        assertEquals(unit, node.getQuantity().unit(), text);
        assertEquals(next, read.next(), text);
    }

    private record Read(Optional<QuantityLiteralNode> quantity, String next) {
    }

    private static Read read(String text) {
        StandardExtensionContainer extensions = new StandardExtensionContainer();
        extensions.importExtension(new CoreExtension());
        extensions.importExtension(new Extension() {
            @Override
            public Set<String> getExpressionBoundaries() {
                return Set.of("in");
            }
        });
        ParserContext context = new DefaultParserContext(extensions);
        TokenCursor cursor = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()))
                .tokenize(new StringSource("quantity", text));
        cursor.consumeIf(BasicToken.T_SOL);
        Optional<QuantityLiteralNode> quantity = ((QuantityParser) context.getParser(QuantityParser.class)).read(cursor, context);
        String next = !cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL) ? "<end>" : cursor.current().value();
        return new Read(quantity, next);
    }
}
