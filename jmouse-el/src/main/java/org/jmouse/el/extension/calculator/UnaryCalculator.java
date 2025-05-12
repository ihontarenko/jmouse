package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.calculator.operation.OperationType;

import java.util.function.UnaryOperator;

/**
 * Enum representing unary operations for expressions.
 * Unary operators operate on a single operand.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum UnaryCalculator implements Calculator<Object> {

    /**
     * Increment operator ({@code ++}), increases a value by 1.
     */
    INCREMENT(new IncrementOperation()),

    /**
     * Decrement operator ({@code --}), decreases a value by 1.
     */
    DECREMENT(new DecrementOperation());

    private final UnaryOperator<Object> operation;

    UnaryCalculator(UnaryOperator<Object> operation) {
        this.operation = operation;
    }

    @Override
    public Object calculate(Object... operands) {
        return operation.apply(operands[0]);
    }

    /**
     * Increment operation ({@code ++}), increases a number by 1.
     */
    public static class IncrementOperation implements UnaryOperator<Object> {
        @Override
        public Object apply(Object operand) {
            return MathematicCalculator.CALCULATION.unary(OperationType.INCREMENT, operand);
        }
    }

    /**
     * Decrement operation ({@code --}), decreases a number by 1.
     */
    public static class DecrementOperation implements UnaryOperator<Object> {
        @Override
        public Object apply(Object operand) {
            return MathematicCalculator.CALCULATION.unary(OperationType.DECREMENT, operand);
        }
    }

}
