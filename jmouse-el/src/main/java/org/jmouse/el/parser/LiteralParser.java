package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
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
                // numeric literal without explicit suffix
                String raw = token.value();

                if (raw.contains(".") || raw.toLowerCase().contains("e")) {
                    BigDecimal big  = new BigDecimal(raw);
                    float      f    = big.floatValue();
                    double     d    = big.doubleValue();
                    BigDecimal bigF = new BigDecimal(Float.toString(f));
                    BigDecimal bigD = BigDecimal.valueOf(d);

                    if (big.compareTo(bigF) == 0) {
                        parent.add(new FloatLiteralNode(f));
                    } else if (big.compareTo(bigD) == 0) {
                        parent.add(new DoubleLiteralNode(d));
                    } else {
                        parent.add(new BigDecimalLiteralNode(big));
                    }

                } else {
                    try {
                        parent.add(new IntegerLiteralNode(Integer.parseInt(token.value())));
                    } catch (NumberFormatException e) {
                        try {
                            parent.add(new LongLiteralNode(Long.parseLong(token.value())));
                        } catch (NumberFormatException ignored) {
                            parent.add(new BigIntegerLiteralNode(new BigInteger(token.value())));
                        }
                    }
                }

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
