package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.calculator.operation.Calculation;
import org.jmouse.el.extension.calculator.operation.handler.IntegerOperationHandler;
import org.jmouse.el.extension.calculator.operation.OperationType;

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

    public final static Calculation CALCULATION = new Calculation();

    static {
        CALCULATION.register(new IntegerOperationHandler());
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
            return CALCULATION.binary(OperationType.PLUS, left, right);
        }
    }

    /** Subtraction operation. */
    public static class SubtractiveOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            return CALCULATION.binary(OperationType.MINUS, left, right);
        }
    }

    /** Multiplication operation. */
    public static class MultiplicativeOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            return CALCULATION.binary(OperationType.MULTIPLY, left, right);
        }
    }

    /** Division operation. */
    public static class DivisionOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            return CALCULATION.binary(OperationType.DIVIDE, left, right);
        }
    }

    /** Modulus operation. */
    public static class ModulusOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            return CALCULATION.binary(OperationType.MODULUS, left, right);
        }
    }

    /** Exponential operation. */
    public static class ExponentialOperation implements BinaryOperator<Object> {
        @Override
        public Object apply(Object left, Object right) {
            return CALCULATION.binary(OperationType.EXPONENTIAL, left, right);
        }
    }

}
