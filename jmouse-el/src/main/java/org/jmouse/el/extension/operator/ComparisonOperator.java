package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.ComparisonCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * Represents comparison operators ({@code >, <, >=, <=, ==, !=}).
 *
 * @author Ivan Hontarenko
 */
public enum ComparisonOperator implements Operator {

    /**
     * Greater than ({@code >}), returns {@code true} if left operand is greater.
     */
    GT(ComparisonCalculator.GT, BasicToken.T_GT, "GREATER_THAN", 7),

    /**
     * Less than ({@code <}), returns {@code true} if left operand is smaller.
     */
    LT(ComparisonCalculator.LT, BasicToken.T_LT, "LESS_THAN", 7),

    /**
     * Greater than or equal ({@code >=}), returns {@code true} if left operand is greater or equal.
     */
    GTE(ComparisonCalculator.GTE, BasicToken.T_GE, "GREATER_THAN_OR_EQUAL", 7),

    /**
     * Less than or equal ({@code <=}), returns {@code true} if left operand is smaller or equal.
     */
    LTE(ComparisonCalculator.LTE, BasicToken.T_LE, "LESS_THAN_OR_EQUAL", 7),

    /**
     * Equal ({@code ==}), returns {@code true} if both operands are equal.
     */
    EQUAL(ComparisonCalculator.EQUAL, BasicToken.T_EQ, "EQUAL", 8),

    /**
     * Not equal ({@code !=}), returns {@code true} if operands are not equal.
     */
    NOT_EQUAL(ComparisonCalculator.NOT_EQUAL, BasicToken.T_NE, "NOT_EQUAL", 8);

    private final Calculator<Boolean> calculator;
    private final Token.Type          type;
    private final String              name;
    private final int                 precedence;

    ComparisonOperator(Calculator<Boolean> calculator, Token.Type type, String name, int precedence) {
        this.calculator = calculator;
        this.type = type;
        this.name = name;
        this.precedence = precedence;
    }

    @Override
    public int getPrecedence() {
        return precedence;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Token.Type getType() {
        return type;
    }

    @Override
    public Calculator<?> getCalculator() {
        return calculator;
    }

}
