package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.UnaryCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * Enum representing **unary operators** used in expressions.
 * Unary operators operate on a **single operand** and have high precedence.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * int x = 5;
 * ++x;   // INCREMENT
 * --x;   // DECREMENT
 * int y = -x; // NEGATE
 * boolean isFalse = !true; // LOGICAL_NEGATION
 * }</pre>
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
    DECREMENT(UnaryCalculator.DECREMENT, BasicToken.T_DECREMENT, "DECREMENT", 40),

    /**
     * Unary plus ({@code +}), preserves the sign of a number.
     * Precedence: 30.
     */
    UNARY_PLUS(UnaryCalculator.UNARY_PLUS, BasicToken.T_PLUS, "UNARY_PLUS", 30),

    /**
     * Unary minus ({@code -}), negates the value of a number.
     * Precedence: 30.
     */
    UNARY_MINUS(UnaryCalculator.UNARY_MINUS, BasicToken.T_MINUS, "UNARY_MINUS", 30),

    /**
     * Logical negation ({@code !}), inverts a boolean value.
     * Precedence: 35 (higher than comparison operators).
     */
    LOGICAL_NEGATION(UnaryCalculator.LOGICAL_NEGATION, BasicToken.T_NEGATE, "LOGICAL_NEGATION", 35);


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
