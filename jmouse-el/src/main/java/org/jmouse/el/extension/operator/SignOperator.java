package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.UnaryCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * The sign written before an operand: {@code -x}.
 *
 * <p>⚠️ Never registered. Operators are looked up by their token, and {@code -} is
 * {@link MathematicOperator#SUBTRACT}'s; whether a minus negates or subtracts is decided by where it stands, so a
 * sign is parsed, not looked up. This operator only names what a negation does.</p>
 */
public enum SignOperator implements Operator {

    NEGATIVE(UnaryCalculator.NEGATE, BasicToken.T_MINUS, "NEGATIVE", 900);

    private final Calculator<Object> calculator;
    private final Token.Type         type;
    private final String             name;
    private final int                precedence;

    SignOperator(Calculator<Object> calculator, Token.Type type, String name, int precedence) {
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
