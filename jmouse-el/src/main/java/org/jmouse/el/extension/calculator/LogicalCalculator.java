package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.util.Arrays;

import java.util.function.BinaryOperator;

/**
 * Enum representing logical operations in expressions.
 * Each operator evaluates boolean conditions.
 *
 * @author Ivan Hontarenko
 */
public enum LogicalCalculator implements Calculator<Boolean> {

    /** Logical AND ({@code &&}). */
    AND(new AndOperation()),

    /** Logical OR ({@code ||}). */
    OR(new OrOperation()),

    /** Logical NOT ({@code !}). */
    NOT(new NotOperation()),

    /** Logical XOR ({@code ^}). */
    XOR(new XorOperation());

    private final BinaryOperator<Boolean> operation;

    LogicalCalculator(BinaryOperator<Boolean> operation) {
        this.operation = operation;
    }

    @Override
    public Boolean calculate(Object... operands) {
        Object valueA = Arrays.get(operands, 0, null);
        Object valueB = Arrays.get(operands, 1, null);

        if (valueA instanceof Boolean booleanA && valueB instanceof Boolean booleanB) {
            return operation.apply(booleanA, booleanB);
        }

        return false;
    }

    /** AND operation */
    public static class AndOperation implements BinaryOperator<Boolean> {
        @Override
        public Boolean apply(Boolean left, Boolean right) {
            return left && right;
        }
    }

    /** OR operation */
    public static class OrOperation implements BinaryOperator<Boolean> {
        @Override
        public Boolean apply(Boolean left, Boolean right) {
            return left || right;
        }
    }

    /** NOT operation */
    public static class NotOperation implements BinaryOperator<Boolean> {
        @Override
        public Boolean apply(Boolean left, Boolean ignored) {
            return !left;
        }
    }

    /** XOR operation */
    public static class XorOperation implements BinaryOperator<Boolean> {
        @Override
        public Boolean apply(Boolean left, Boolean right) {
            return left ^ right;
        }
    }

}
