package org.jmouse.el.core.extension.operator;

import org.jmouse.el.core.extension.Calculator;
import org.jmouse.el.core.extension.Operator;
import org.jmouse.el.core.extension.calculator.UnaryCalculator;
import org.jmouse.el.core.lexer.BasicToken;
import org.jmouse.el.core.lexer.Token;

/**
 * Enum representing **unary operators** used in expressions.
 * Unary operators operate on a **single operand** and have high precedence.
 *
 * <p>Operators with higher precedence are evaluated first.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum UnaryOperator implements Operator {

    /**
     * Increment operator ({@code ++}), increases a value by 1.
     * Precedence: 40 (higher than binary operators).
     */
    INCREMENT(UnaryCalculator.INCREMENT, BasicToken.T_INCREMENT, "INCREMENT", 40),

    /**
     * Decrement operator ({@code --}), decreases a value by 1.
     * Precedence: 40 (higher than binary operators).
     */
    DECREMENT(UnaryCalculator.DECREMENT, BasicToken.T_DECREMENT, "DECREMENT", 40);

    private final Calculator<Object> calculator;
    private final Token.Type         type;
    private final String             name;
    private final int                precedence;

    /**
     * Constructs a new {@link UnaryOperator}.
     */
    UnaryOperator(Calculator<Object> calculator, Token.Type type, String name, int precedence) {
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
