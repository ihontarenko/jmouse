package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * 🔢 Enum representing binary operators used in expressions.
 * Each operator has a token type, a name, and a precedence level that determines
 * the order of evaluation in arithmetic expressions.
 * <p>
 * Operators with <b>higher precedence</b> are evaluated first.
 * <p>
 * Example precedence order (higher is evaluated first):
 * <pre>
 *     1 + 2 * 3  → MULTIPLY (20) before PLUS (10)
 *     2 ^ 3 + 1  → EXPONENTIAL (30) before PLUS (10)
 * </pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum BinaryOperator implements Operator {

    /**
     * ➕ Addition operator (`+`), used for summation.
     * Precedence: 10
     */
    PLUS(BasicToken.T_PLUS, "PLUS", 10),

    /**
     * ➖ Subtraction operator (`-`), used for difference calculations.
     * Precedence: 10
     */
    SUBTRACT(BasicToken.T_MINUS, "SUBTRACT", 10),

    /**
     * ✖️ Multiplication operator (`*`), used for product calculations.
     * Precedence: 20
     */
    MULTIPLY(BasicToken.T_MULTIPLY, "MULTIPLY", 20),

    /**
     * ➗ Division operator (`/`), used for quotient calculations.
     * Precedence: 20
     */
    DIVIDE(BasicToken.T_DIVIDE, "DIVIDE", 20),

    /**
     * 🔢 Modulus operator (`%`), returns the remainder of a division.
     * Precedence: 20 (same as multiplication and division)
     */
    MODULUS(BasicToken.T_PERCENT, "MODULUS", 20),
    /**
     * 🔼 Exponential operator (`^`), used for power calculations.
     * Precedence: 30 (highest among binary operators)
     */
    EXPONENTIAL(BasicToken.T_CARET, "EXPONENTIAL", 30);

    /** 🎭 The token type representing the operator. */
    private final Token.Type type;

    /** 🏷️ The human-readable name of the operator. */
    private final String name;

    /** ⚖️ The precedence level determining the order of operations. */
    private final int precedence;

    /**
     * 🎭 Constructs a new {@link BinaryOperator}.
     *
     * @param type 🔠 the token type of the operator
     * @param name 🏷️ the name of the operator
     * @param precedence ⚖️ the precedence level of the operator
     */
    BinaryOperator(Token.Type type, String name, int precedence) {
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
