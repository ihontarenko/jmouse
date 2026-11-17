package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.el.node.expression.literal.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import static java.lang.Float.MAX_VALUE;
import static java.lang.Float.MIN_NORMAL;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static org.jmouse.el.lexer.BasicToken.*;

/**
 * Parses literal tokens (string, boolean, null, character, and numeric literals)
 * and converts them into corresponding AST nodes.
 * <p>
 * Numeric literals may include an optional type suffix (B, S, I, L, F, D, C).
 * When no suffix is present (token type T_NUMERIC), the parser distinguishes
 * between integer (int/long) and floating-point (float/double) based on content
 * and range checks.
 * </p>
 * <p>Example literals:</p>
 * <pre>{@code
 *   42          // int or long
 *   42L         // long
 *   3.14        // float or double
 *   2.5E2F      // float
 *   'x'         // character
 *   "hello"    // string
 *   true        // boolean
 *   null        // null
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class LiteralParser implements Parser {

    /**
     * The literal of a number written with no type suffix — an optional sign, digits, a fraction, an exponent: the narrowest
     * of {@code int}, {@code long} and a big integer for a whole one; a {@code double} for a decimal, or a big decimal where a
     * double would not hold it exactly. Every language on the engine reads an unsuffixed number by this one rule.
     *
     * @param written the number as written
     * @return its literal node
     */
    /** How many significant decimal digits a double always reads back unchanged. */
    private static final int DOUBLE_KEPT_DIGITS = 15;

    /** The significant digits of a decimal's mantissa: its digits less the zeros leading and trailing them. */
    private static int significantDigits(String written) {
        int first = -1;
        int last  = -1;
        int count = 0;
        for (int index = 0; index < written.length(); index++) {
            char character = written.charAt(index);
            if (character == 'e' || character == 'E') {
                break;
            }
            if (character >= '0' && character <= '9') {
                if (character != '0') {
                    if (first < 0) {
                        first = count;
                    }
                    last = count;
                }
                count++;
            }
        }
        return first < 0 ? 0 : last - first + 1;
    }

    public static LiteralNode<?> numericLiteral(String written) {
        if (written.contains(".") || written.toLowerCase().contains("e")) {
            // ⚠️ A decimal with no suffix is a DOUBLE, and choosing the narrowest type that
            // happens to round-trip its text was quietly wrong. `0.8` prints back exactly as a
            // float, so it became one — and `500 * 0.8` then answered 400.0000059604645 while
            // `500 * 0.8d` answered 400.0. Nothing raised; a total was simply a little wrong.
            //
            // Java, and every language somebody arrives here from, reads an unsuffixed decimal as
            // a double. Somebody who wants a float writes `0.8f`, which the suffix path handles.
            //
            // A decimal of at most fifteen significant digits whose double is finite and normal reads back as exactly that
            // double — fifteen is the most digits a double always keeps —, so it is one with no BigDecimal built to ask.
            if (significantDigits(written) <= DOUBLE_KEPT_DIGITS) {
                double number = Double.parseDouble(written);
                if (number == 0 || Double.isFinite(number) && Math.abs(number) >= Double.MIN_NORMAL) {
                    return new DoubleLiteralNode(number);
                }
            }

            BigDecimal big    = new BigDecimal(written);
            double     number = big.doubleValue();

            // A decimal past a double's reach is no double at all: it stays as written.
            if (!Double.isFinite(number)) {
                return new BigDecimalLiteralNode(big);
            }

            return big.compareTo(BigDecimal.valueOf(number)) == 0 ? new DoubleLiteralNode(number) : new BigDecimalLiteralNode(big);
        }

        try {
            return new IntegerLiteralNode(Integer.parseInt(written));
        } catch (NumberFormatException notAnInteger) {
            try {
                return new LongLiteralNode(Long.parseLong(written));
            } catch (NumberFormatException notALong) {
                return new BigIntegerLiteralNode(new BigInteger(written));
            }
        }
    }

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        // Peek at the next token without advancing the cursor.
        Token      token = cursor.peek();
        BasicToken type  = (BasicToken) token.type();

        // Ensure the token is one of the expected literal types.
        cursor.ensure(T_NUMERIC, T_STRING, T_TRUE, T_FALSE, T_NULL,
                      T_BYTE, T_SHORT, T_CHARACTER, T_INT, T_LONG, T_FLOAT, T_DOUBLE);

        // Create and add the appropriate literal node based on the token type.
        switch (type) {
            case T_NULL:
                parent.add(new NullLiteralNode());
                break;
            case T_STRING:
                parent.add(new StringLiteralNode(token.value()));
                break;
            case T_TRUE:
            case T_FALSE:
                parent.add(new BooleanLiteralNode(type == T_TRUE));
                break;
            case T_NUMERIC:
                parent.add(numericLiteral(token.value()));
                break;

            case T_BYTE:
                // suffix B/b indicates byte literal
                parent.add(new ByteLiteralNode(Byte.parseByte(token.value())));
                break;
            case T_CHARACTER:
                // character literal, token.value() includes quotes
                parent.add(new CharacterLiteralNode((char) Byte.parseByte(token.value())));
                break;
            case T_SHORT:
                // suffix S/s indicates short literal
                parent.add(new ShortLiteralNode(Short.parseShort(token.value())));
                break;
            case T_INT:
                // suffix I/i indicates integer literal
                parent.add(new IntegerLiteralNode(Integer.parseInt(token.value())));
                break;
            case T_LONG:
                // suffix L/l indicates long literal
                parent.add(new LongLiteralNode(Long.parseLong(token.value())));
                break;
            case T_FLOAT:
                // suffix F/f indicates float literal
                parent.add(new FloatLiteralNode(Float.parseFloat(token.value())));
                break;
            case T_DOUBLE:
                // suffix D/d indicates double literal
                parent.add(new DoubleLiteralNode(Double.parseDouble(token.value())));
                break;

        }
    }
}
