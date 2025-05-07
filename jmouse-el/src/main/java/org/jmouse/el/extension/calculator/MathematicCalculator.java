package org.jmouse.el.extension.calculator;

import org.jmouse.core.reflection.Reflections;
import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.calculator.mathematic.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * Enum representing arithmetic operations for expressions.
 * Each operator performs a specific calculation on numeric operands.
 *
 * @author Ivan Hontarenko
 */
public enum MathematicCalculator implements Calculator<Object> {

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

    private final BinaryOperator<Object> operation;

    public static final Map<Class<?>, MathematicOperation<?>> OPERATIONS;

    static {
        OPERATIONS = new HashMap<>();
        OPERATIONS.put(Integer.class, new IntegerOperation());
        OPERATIONS.put(Float.class, new FloatOperation());
        OPERATIONS.put(Double.class, new DoubleOperation());
        OPERATIONS.put(Long.class, new LongOperation());
    }

    /**
     * Constructs a new {@link MathematicCalculator} operator.
     *
     * @param operation The mathematical operation implementation.
     */
    MathematicCalculator(BinaryOperator<Object> operation) {
        this.operation = operation;
    }

    @Override
    public Object calculate(Object... operands) {
        return operation.apply(operands[0], operands[1]);
    }

    /** Addition operation. */
    public static class AdditiveOperation implements BinaryOperator<Object> {

        @Override
        public Object apply(Object left, Object right) {
            MathematicOperation<Object> operation = (MathematicOperation<Object>) MathematicCalculator.OPERATIONS.get(left.getClass());

            if (operation != null) {
                return operation.plus(left, right);
            }

            throw new MathematicOperationException(
                    "Addition not supported for %s and %s"
                            .formatted(Reflections.describe(left), Reflections.describe(right)));
        }
    }

    /** Subtraction operation. */
    public static class SubtractiveOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            MathematicOperation<Object> operation = (MathematicOperation<Object>) MathematicCalculator.OPERATIONS.get(left.getClass());

            if (operation != null) {
                return operation.minus(left, right);
            }

            throw new MathematicOperationException(
                    "Subtraction not supported for %s and %s"
                            .formatted(Reflections.describe(left), Reflections.describe(right)));
        }
    }

    /** Multiplication operation. */
    public static class MultiplicativeOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            MathematicOperation<Object> operation = (MathematicOperation<Object>) MathematicCalculator.OPERATIONS.get(left.getClass());

            if (operation != null) {
                return operation.multiply(left, right);
            }

            throw new MathematicOperationException(
                    "Multiplication not supported for %s and %s"
                            .formatted(Reflections.describe(left), Reflections.describe(right)));
        }
    }

    /** Division operation. */
    public static class DivisionOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            MathematicOperation<Object> operation = (MathematicOperation<Object>) MathematicCalculator.OPERATIONS.get(left.getClass());

            if (operation != null) {
                return operation.divide(left, right);
            }

            throw new MathematicOperationException(
                    "Division not supported for %s and %s"
                            .formatted(Reflections.describe(left), Reflections.describe(right)));
        }
    }

    /** Modulus operation. */
    public static class ModulusOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            MathematicOperation<Object> operation = (MathematicOperation<Object>) MathematicCalculator.OPERATIONS.get(left.getClass());

            if (operation != null) {
                return operation.modulus(left, right);
            }

            throw new MathematicOperationException(
                    "Modulus not supported for %s and %s"
                            .formatted(Reflections.describe(left), Reflections.describe(right)));
        }
    }

    /** Exponential operation. */
    public static class ExponentialOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            MathematicOperation<Object> operation = (MathematicOperation<Object>) MathematicCalculator.OPERATIONS.get(left.getClass());

            if (operation != null) {
                return operation.exponential(left, right);
            }

            throw new MathematicOperationException(
                    "Exponentiation not supported for %s and %s"
                            .formatted(Reflections.describe(left), Reflections.describe(right)));
        }
    }

}
