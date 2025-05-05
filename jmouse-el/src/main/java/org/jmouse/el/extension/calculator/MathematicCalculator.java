package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.calculator.mathematic.IntegerAdditiveOperator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * Enum representing arithmetic operations for expressions.
 * Each operator performs a specific calculation on numeric operands.
 *
 * @author Ivan Hontarenko
 */
public enum MathematicCalculator implements Calculator<Number> {

    /**
     * Addition ({@code +}), returns the sum of two numbers.
     */
    PLUS(new AdditiveOperation()),

    /**
     * Subtraction ({@code -}), returns the difference between two numbers.
     */
    SUBTRACT(new SubtractiveOperation()),

    /**
     * Multiplication ({@code *}), returns the product of two numbers.
     */
    MULTIPLY(new MultiplicativeOperation()),

    /**
     * Division ({@code /}), returns the quotient of two numbers.
     * Returns {@link Double#NaN} if division by zero occurs.
     */
    DIVIDE(new DivisionOperation()),

    /**
     * Modulus ({@code %}), returns the remainder of division.
     */
    MODULUS(new ModulusOperation()),

    /**
     * Exponential ({@code ^}), returns {@code a^b}.
     */
    EXPONENTIAL(new ExponentialOperation());

    private final BinaryOperator<Number> operation;

    /**
     * Constructs a new {@link MathematicCalculator} operator.
     *
     * @param operation The mathematical operation implementation.
     */
    MathematicCalculator(BinaryOperator<Number> operation) {
        this.operation = operation;
    }

    @Override
    public Number calculate(Object... operands) {
        return operation.apply((Number) operands[0], (Number) operands[1]);
    }

    /** Addition operation. */
    public static class AdditiveOperation implements BinaryOperator<Number> {

        public static final Map<Class<?>, BinaryOperator<Object>> OPERATORS = new HashMap<>();

        static {
            OPERATORS.put(Integer.class, new IntegerAdditiveOperator());
        }

        @Override
        public Number apply(Number left, Number right) {
            BinaryOperator<Object> binaryOperator = OPERATORS.get(left.getClass());

            if (binaryOperator != null) {
                // return binaryOperator.apply(left, right);
            }

            return left.doubleValue() + right.doubleValue();
        }
    }

    /** Subtraction operation. */
    public static class SubtractiveOperation implements BinaryOperator<Number> {
        @Override
        public Number apply(Number left, Number right) {
            return left.doubleValue() - right.doubleValue();
        }
    }

    /** Multiplication operation. */
    public static class MultiplicativeOperation implements BinaryOperator<Number> {
        @Override
        public Number apply(Number left, Number right) {
            return left.doubleValue() * right.doubleValue();
        }
    }

    /** Division operation. */
    public static class DivisionOperation implements BinaryOperator<Number> {
        @Override
        public Number apply(Number left, Number right) {
            return right.doubleValue() == 0 ? Double.NaN : left.doubleValue() / right.doubleValue();
        }
    }

    /** Modulus operation. */
    public static class ModulusOperation implements BinaryOperator<Number> {
        @Override
        public Number apply(Number left, Number right) {
            return left.doubleValue() % right.doubleValue();
        }
    }

    /** Exponential operation. */
    public static class ExponentialOperation implements BinaryOperator<Number> {
        @Override
        public Number apply(Number left, Number right) {
            return Math.pow(left.doubleValue(), right.doubleValue());
        }
    }

}
