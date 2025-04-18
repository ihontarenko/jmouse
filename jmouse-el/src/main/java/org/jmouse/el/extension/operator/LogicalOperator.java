package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.LogicalCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * Enum representing logical operators.
 * Logical operators evaluate boolean expressions.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public enum LogicalOperator implements Operator {

    /**
     * Logical AND ({@code &&}).
     */
    AND(LogicalCalculator.AND, BasicToken.T_AND, "AND", 5),

    /**
     * Logical OR ({@code ||}).
     */
    OR(LogicalCalculator.OR, BasicToken.T_OR, "OR", 4),

    /**
     * Logical NOT ({@code !}).
     */
    NOT(LogicalCalculator.NOT, BasicToken.T_NEGATE, "NOT", 6),

    /**
     * Logical XOR ({@code ^}).
     */
    XOR(LogicalCalculator.XOR, BasicToken.T_CARET, "XOR", 5);

    private final Calculator<Boolean> calculator;
    private final Token.Type          type;
    private final String              name;
    private final int                 precedence;

    LogicalOperator(Calculator<Boolean> calculator, Token.Type type, String name, int precedence) {
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
