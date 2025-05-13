package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.NullCoalesceCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * Represents the "??" null-coalesce operator ({@code IS}).
 * <p>
 * Example:
 * <pre>{@code
 *     user.name ?? "Guest"
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
public enum NullCoalesceOperator implements Operator {

    /**
     * Test operator ({@code NULL_COALESCE}), used for applying tests to values.
     */
    NULL_COALESCE(new NullCoalesceCalculator(), BasicToken.T_NULL_COALESCE, "??", 10);

    private final Calculator<Object> calculator;
    private final Token.Type         type;
    private final String             name;
    private final int                precedence;

    NullCoalesceOperator(Calculator<Object> calculator, Token.Type type, String name, int precedence) {
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
