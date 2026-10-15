package org.jmouse.formula.el;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.MathematicCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.formula.MathematicalFunction;

/**
 * The operators jMF adds to jME. {@code ^} is registered on jME's caret token, so in a formula container it
 * replaces XOR: in mathematics a caret is a power. {@code NEGATE} and {@code FACTORIAL} are never registered — they
 * only name the operators of a {@link NegationNode} and a {@link FactorialNode}, because a leading minus and a
 * trailing {@code !} are parsed, not looked up.
 */
public enum FormulaOperator implements Operator {

    DIVIDE(BasicToken.T_DIVIDE, "DIVIDE", 700, FormulaOperator::divide),
    POWER(BasicToken.T_CARET, "POWER", 800, operands -> Math.pow(number(operands[0]), number(operands[1]))),
    NEGATE(BasicToken.T_MINUS, "NEGATE", 900, operands -> -number(operands[0])),
    FACTORIAL(BasicToken.T_NEGATE, "FACTORIAL", 1000,
            operands -> MathematicalFunction.FACTORIAL.real().apply(new double[]{number(operands[0])}));

    private final Token.Type type;
    private final String name;
    private final int precedence;
    private final Calculator<Object> calculator;

    FormulaOperator(Token.Type type, String name, int precedence, Calculator<Object> calculator) {
        this.type = type;
        this.name = name;
        this.precedence = precedence;
        this.calculator = calculator;
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

    /** Two whole numbers divide to their exact quotient: whole when it is whole, real otherwise. */
    private static Object divide(Object... operands) {
        if (isWhole(operands[0]) && isWhole(operands[1])) {
            long dividend = ((Number) operands[0]).longValue();
            long divisor = ((Number) operands[1]).longValue();
            if (divisor != 0 && dividend % divisor == 0) {
                return MathematicCalculator.DIVIDE.calculate(operands);
            }
            return number(operands[0]) / number(operands[1]);
        }
        return MathematicCalculator.DIVIDE.calculate(operands);
    }

    private static boolean isWhole(Object operand) {
        return operand instanceof Integer || operand instanceof Long
                || operand instanceof Short || operand instanceof Byte;
    }

    private static double number(Object operand) {
        if (operand instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("A formula operator works on numbers, not on " + operand);
    }
}
