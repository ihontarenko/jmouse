package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.RangeCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * Represents the ".." range operator ({@code T_DOUBLE_DOT}).
 * <p>
 * Example:
 * <pre>{@code
 *     2 .. 4 + 5
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
public enum RangeOperator implements Operator {

    /**
     * Test operator ({@code ..}), used for applying tests to values.
     */
    RANGE(new RangeCalculator(), BasicToken.T_DOUBLE_DOT, "..", 650);

    private final Calculator<Object> calculator;
    private final Token.Type         type;
    private final String             name;
    private final int                precedence;

    RangeOperator(Calculator<Object> calculator, Token.Type type, String name, int precedence) {
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
