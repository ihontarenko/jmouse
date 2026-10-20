package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.units.Quantity;
import org.jmouse.units.Units;

import java.util.Optional;
import java.util.Set;

/**
 * Reads a number and, at most, its unit — {@code 100 nF}, {@code 2.2µs}, {@code 200 ppm/K}, {@code -40 °C},
 * {@code - 5}, {@code 25} — into a {@link QuantityLiteralNode}. The units are {@link Units}' table.
 *
 * <p>⚠️ <strong>Registered by default, never chosen by dispatch</strong> ({@link #supports} is false): whether a
 * number followed by a word is a quantity is the language's grammar, not the expression's — in a formula
 * {@code 2 m} may be twice a variable. A language asks it where its grammar allows a value with a unit, through
 * {@link #read}.</p>
 */
public class QuantityParser implements Parser {

    /** What ends a unit even where it touches it: {@code 5 V, 6 V} is two values. */
    private static final Set<String> SEPARATORS = Set.of(",", ";", "=", "(", ")", "[", "]", "{", "}", ":");

    private static final Set<BasicToken> NUMBERS = Set.of(BasicToken.T_NUMERIC, BasicToken.T_INT, BasicToken.T_LONG,
            BasicToken.T_FLOAT, BasicToken.T_DOUBLE, BasicToken.T_SHORT, BasicToken.T_BYTE);

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Token at = cursor.hasNext() ? cursor.current() : null;
        parent.add(read(cursor, context).orElseThrow(() -> new ParseException(
                "a number with, at most, its unit was expected" + (at == null ? "" : ", not '" + at.value() + "'"))));
    }

    /**
     * Reads a quantity where the cursor stands; the cursor is left after it. Empty, the cursor where it was, when no
     * number stands there.
     */
    public Optional<QuantityLiteralNode> read(TokenCursor cursor, ParserContext context) {
        TokenCursor.Savepoint start = cursor.savepoint();
        Optional<String> number = number(cursor);
        if (number.isEmpty()) {
            cursor.restore(start);
            return Optional.empty();
        }
        TokenCursor.Savepoint afterNumber = cursor.savepoint();
        if (atUnit(cursor, context)) {
            boolean joined = SourceReading.touches(cursor, cursor.lookAt(-1), cursor.current());
            String unit = SourceReading.touchingWord(cursor, SEPARATORS);
            Optional<Quantity> quantity = Quantity.of(Double.parseDouble(number.get()), unit);
            if (quantity.isPresent()) {
                return Optional.of(new QuantityLiteralNode(quantity.get()));
            }
            if (joined) {
                // Written against the number and not a unit — 74hc595, 2pi —: the whole is a word or a formula.
                cursor.restore(start);
                return Optional.empty();
            }
            cursor.restore(afterNumber);
        }
        return Optional.of(new QuantityLiteralNode(Quantity.plain(Double.parseDouble(number.get()))));
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return false;
    }

    /** A number, its sign allowed to stand apart: {@code -0.8}, {@code - 5}, {@code 1e-9}. */
    private static Optional<String> number(TokenCursor cursor) {
        String sign = "";
        if (cursor.isCurrent(BasicToken.T_MINUS, BasicToken.T_PLUS) && isNumber(cursor.lookAt(1))) {
            sign = cursor.next().value();
        }
        if (!cursor.hasNext() || !isNumber(cursor.current())) {
            return Optional.empty();
        }
        String written = cursor.next().value();
        return Optional.of(sign.equals("-") ? negated(written) : written);
    }

    private static String negated(String written) {
        return written.startsWith("-") ? written.substring(1) : "-" + written;
    }

    private static boolean isNumber(Token token) {
        return token != null && token.type() instanceof BasicToken basic && NUMBERS.contains(basic);
    }

    /** Whether a unit may begin where the cursor stands: a word, not a separator, a text or a language's boundary. */
    private static boolean atUnit(TokenCursor cursor, ParserContext context) {
        if (!cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL, BasicToken.T_NEW_LINE, BasicToken.T_STRING)) {
            return false;
        }
        Token token = cursor.current();
        return !SEPARATORS.contains(token.value()) && !context.endsExpression(cursor);
    }
}
