package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.literal.*;

import java.math.BigDecimal;

import static java.lang.Float.MAX_VALUE;
import static java.lang.Float.MIN_NORMAL;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static org.jmouse.el.lexer.BasicToken.*;

/**
 * Parses literal tokens and converts them into corresponding literal AST nodes.
 * <p>
 * The {@code LiteralParser} handles numeric, string, boolean, and null literals.
 * It examines the next token from the {@link TokenCursor}, ensures it is one of the expected
 * literal types, and then creates an appropriate literal node which is added to the parent node.
 * </p>
 */
public class LiteralParser implements Parser {

    /**
     * Parses a literal token from the provided cursor and adds a corresponding literal node to the parent node.
     * <p>
     * Supported literal tokens include:
     * <ul>
     *   <li>{@code T_INT} – parsed as a {@link LongLiteralNode}</li>
     *   <li>{@code T_FLOAT} – parsed as a {@link DoubleLiteralNode}</li>
     *   <li>{@code T_STRING} – parsed as a {@link StringLiteralNode}</li>
     *   <li>{@code T_TRUE} and {@code T_FALSE} – parsed as a {@link BooleanLiteralNode}</li>
     *   <li>{@code T_NULL} – parsed as a {@link NullLiteralNode}</li>
     * </ul>
     * </p>
     *
     * @param cursor  the token cursor from which to read the literal token
     * @param parent  the parent node to which the parsed literal node should be added
     * @param context the parser context for accessing additional parsers or configuration
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        // Peek at the next token without advancing the cursor.
        Token      token = cursor.peek();
        BasicToken type  = (BasicToken) token.type();

        // Ensure the token is one of the expected literal types.
        cursor.ensure(BasicToken.T_INT, T_FLOAT, T_STRING, T_TRUE, T_FALSE, T_NULL);

        // Create and add the appropriate literal node based on the token type.
        switch (type) {
            case T_NULL:
                parent.add(new NullLiteralNode());
                break;
            case T_STRING:
                parent.add(new StringLiteralNode(token.value()));
                break;
            case T_INT:

                try {
                    parent.add(new IntegerLiteralNode(Integer.parseInt(token.value())));
                } catch (NumberFormatException e) {
                    try {
                        parent.add(new LongLiteralNode(Long.parseLong(token.value())));
                    } catch (NumberFormatException ignored) { }
                }

                break;
            case T_FLOAT:

                BigDecimal decimal = new BigDecimal(token.value()).abs();

                boolean inRange = decimal.compareTo(valueOf(MAX_VALUE)) <= 0
                        && (decimal.compareTo(valueOf(MIN_NORMAL)) >= 0 || decimal.compareTo(ZERO) == 0);

                if (decimal.precision() > 7 && inRange) {
                    parent.add(new FloatLiteralNode(Float.parseFloat(token.value())));
                } else {
                    parent.add(new DoubleLiteralNode(Double.parseDouble(token.value())));
                }

                break;
            case T_TRUE:
            case T_FALSE:
                parent.add(new BooleanLiteralNode(type == T_TRUE));
                break;
        }
    }
}
