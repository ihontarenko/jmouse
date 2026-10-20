package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.RangeCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * Represents the ".." range operator ({@code T_DOUBLE_DOT}).
 * <p>
 * Its bounds are whole arithmetic expressions: it binds looser than {@code + - * / % **}, so {@code 0 .. n - 1} is
 * {@code 0 .. (n - 1)} and {@code 2 .. 4 + 5} is {@code 2 .. 9}; tighter than {@code ~}, the comparisons and {@code in},
 * so {@code x in 1 .. n} tests membership in the range.
 * <pre>{@code
 *     for index in 0 .. count - 1 do … end
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
public enum RangeOperator implements Operator {

    /**
     * The range operator ({@code ..}): every whole number from its left bound to its right one.
     */
    RANGE(new RangeCalculator(), BasicToken.T_DOUBLE_DOT, "..", 575);

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
