package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * 🔢 Enum representing unary operators used in expressions.
 * Unary operators act on a **single operand** and have a high precedence level.
 *
 * Example:
 * <pre>
 *     int x = 5;
 *     ++x; // INCREMENT
 *     x--; // DECREMENT
 * </pre>
 *
 * Operators with **higher precedence** are evaluated first.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum UnaryOperator implements Operator {

    /**
     * ➕ Increment operator (`++`), used for increasing a value by 1.
     * Precedence: 40 (higher than binary operators)
     */
    INCREMENT(BasicToken.T_INCREMENT, "INCREMENT", 40),

    /**
     * ➖ Decrement operator (`--`), used for decreasing a value by 1.
     * Precedence: 40 (higher than binary operators)
     */
    DECREMENT(BasicToken.T_DECREMENT, "DECREMENT", 40);

    /** 🎭 The token type representing the operator. */
    private final Token.Type type;

    /** 🏷️ The human-readable name of the operator. */
    private final String name;

    /** ⚖️ The precedence level determining the order of operations. */
    private final int precedence;

    /**
     * 🎭 Constructs a new {@link UnaryOperator}.
     *
     * @param type 🔠 the token type of the operator
     * @param name 🏷️ the name of the operator
     * @param precedence ⚖️ the precedence level of the operator
     */
    UnaryOperator(Token.Type type, String name, int precedence) {
        this.type = type;
        this.name = name;
        this.precedence = precedence;
    }

    /**
     * ⚖️ Retrieves the precedence level of the operator.
     *
     * @return 🔢 the precedence level (higher values indicate higher priority)
     */
    @Override
    public int getPrecedence() {
        return precedence;
    }

    /**
     * 🏷️ Retrieves the name of the operator.
     *
     * @return 📛 the name of the operator
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 🔠 Retrieves the {@link Token.Type} associated with this operator.
     *
     * @return 🎭 the token type representing the operator
     */
    @Override
    public Token.Type getType() {
        return type;
    }
}
