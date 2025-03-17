package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * 🔢 Enum representing logical operators used in expressions.
 * Logical operators are used for boolean logic evaluations.
 *
 * Example:
 * <pre>
 *     (x > 5) && (y < 10) // AND operator
 *     (x == 5) || (y != 3) // OR operator
 * </pre>
 *
 * Operators with **higher precedence** are evaluated first.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum LogicalOperator implements Operator {

    /**
     * 🟢 Logical AND (`&&`), returns {@code true} if both conditions are true.
     * Precedence: 5 (lower than relational operators)
     */
    AND(BasicToken.T_AND, "AND", 5),

    /**
     * 🔴 Logical OR (`||`), returns {@code true} if at least one condition is true.
     * Precedence: 4 (lower than AND)
     */
    OR(BasicToken.T_OR, "OR", 4),

    /**
     * 🔄 Logical NOT (`!`), inverts the boolean value of an tag.
     * Precedence: 6 (higher than AND, OR)
     */
    NOT(BasicToken.T_NEGATE, "NOT", 6),

    /**
     * 🔼 Greater than (`>`), returns {@code true} if left operand is greater than right.
     * Precedence: 7 (higher than AND, OR)
     */
    GT(BasicToken.T_GT, "GREATER_THAN", 7),

    /**
     * 🔽 Less than (`<`), returns {@code true} if left operand is less than right.
     * Precedence: 7 (same as GT)
     */
    LT(BasicToken.T_LT, "LESS_THAN", 7),

    /**
     * 🔼➖ Greater than or equal (`>=`), returns {@code true} if left operand is greater or equal.
     * Precedence: 7
     */
    GTE(BasicToken.T_GE, "GREATER_THAN_OR_EQUAL", 7),

    /**
     * 🔽➖ Less than or equal (`<=`), returns {@code true} if left operand is less or equal.
     * Precedence: 7
     */
    LTE(BasicToken.T_LE, "LESS_THAN_OR_EQUAL", 7),

    /**
     * 🔁 Equal to (`==`), returns {@code true} if both operands are equal.
     * Precedence: 8 (higher than relational operators)
     */
    EQUAL(BasicToken.T_EQ, "EQUAL", 8),

    /**
     * ❌ Not equal to (`!=`), returns {@code true} if operands are not equal.
     * Precedence: 8 (same as EQUAL)
     */
    NOT_EQUAL(BasicToken.T_NE, "NOT_EQUAL", 8),

    /**
     * 🧪 Logical "IS" operator (`IS`), used to apply tests to values.
     * Example: `x IS even`, `name IS not empty`
     * Precedence: 9 (higher than equality and relational operators)
     */
    IS(BasicToken.T_IS, "IS", 9);

    /** 🎭 The token type representing the operator. */
    private final Token.Type type;

    /** 🏷️ The human-readable name of the operator. */
    private final String name;

    /** ⚖️ The precedence level determining the order of operations. */
    private final int precedence;

    /**
     * 🎭 Constructs a new {@link LogicalOperator}.
     *
     * @param type 🔠 the token type of the operator
     * @param name 🏷️ the name of the operator
     * @param precedence ⚖️ the precedence level of the operator
     */
    LogicalOperator(Token.Type type, String name, int precedence) {
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
