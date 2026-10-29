package org.jmouse.formula.el;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.MathematicCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.formula.MathematicalFunction;

/**
 * The operators jMF adds to jME. {@code ^} is registered on jME's caret token, so in a formula container it
 * replaces XOR: in mathematics a caret is a power. {@code FACTORIAL} is never registered — it only
 * names the operator of a {@link FactorialNode}, because a trailing {@code !} is parsed, not looked up; a leading
 * minus is jME's own negation. {@code PAIR} is registered only by a language that reads pairs
 * ({@link FormulaLanguage#withPairs}): {@code a -> b} among a call's arguments is the two of them.
 */
public enum FormulaOperator implements Operator {

    DIVIDE(BasicToken.T_DIVIDE, "DIVIDE", 700, FormulaOperator::divide),
    POWER(BasicToken.T_CARET, "POWER", 800, operands -> Math.pow(number(operands[0]), number(operands[1]))),
    FACTORIAL(BasicToken.T_NEGATE, "FACTORIAL", 1000,
            operands -> MathematicalFunction.FACTORIAL.real().apply(new double[]{number(operands[0])})),
    /** {@code a -> b}: two values written as one pair, the loosest of all — {@code 0 mA -> 0} pairs the whole of each side. */
    PAIR(BasicToken.T_ARROW, "PAIR", 50, operands -> {
        throw new IllegalArgumentException("a pair 'a -> b' is no value: it stands among a call's arguments");
    });

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
