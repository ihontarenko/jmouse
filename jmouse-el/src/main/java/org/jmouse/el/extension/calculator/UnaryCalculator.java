package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;

import java.util.function.UnaryOperator;

/**
 * Enum representing unary operations for expressions.
 * Unary operators operate on a single operand.
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * int x = 5;
 * int y = -x; // NEGATE
 * boolean isFalse = !true; // LOGICAL_NEGATION
 * }
 * </pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum UnaryCalculator implements Calculator<Object> {

    /**
     * Unary plus ({@code +}), preserves the sign of a number.
     */
    UNARY_PLUS(new UnaryPlusOperation()),

    /**
     * Unary minus ({@code -}), negates the value of a number.
     */
    UNARY_MINUS(new UnaryMinusOperation()),

    /**
     * Logical negation ({@code !}), inverts a boolean value.
     */
    LOGICAL_NEGATION(new LogicalNegationOperation()),

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
     * Unary plus operation ({@code +}), preserves the sign of a number.
     */
    public static class UnaryPlusOperation implements UnaryOperator<Object> {
        @Override
        public Object apply(Object operand) {
            if (operand instanceof Number number) {
                return Math.abs(number.doubleValue());
            }

            throw new UnsupportedOperationException("Unsupported operand for UNARY_PLUS: " + operand);
        }
    }

    /**
     * Unary minus operation ({@code -}), negates the value of a number.
     */
    public static class UnaryMinusOperation implements UnaryOperator<Object> {
        @Override
        public Object apply(Object operand) {
            if (operand instanceof Number number) {
                return -Math.abs(number.doubleValue());
            }

            throw new UnsupportedOperationException("Unsupported operand for UNARY_MINUS: " + operand);
        }
    }

    /**
     * Logical negation operation ({@code !}), inverts a boolean value.
     */
    public static class LogicalNegationOperation implements UnaryOperator<Object> {
        @Override
        public Object apply(Object operand) {
            if (operand instanceof Boolean booleanValue) {
                return !booleanValue;
            }

            throw new UnsupportedOperationException("Unsupported operand for LOGICAL_NEGATION: " + operand);
        }
    }

    /**
     * Increment operation ({@code ++}), increases a number by 1.
     */
    public static class IncrementOperation implements UnaryOperator<Object> {
        @Override
        public Object apply(Object operand) {
            if (operand instanceof Number number) {
                return number.doubleValue() + 1;
            }

            throw new UnsupportedOperationException("Unsupported operand for INCREMENT: " + operand);
        }
    }

    /**
     * Decrement operation ({@code --}), decreases a number by 1.
     */
    public static class DecrementOperation implements UnaryOperator<Object> {
        @Override
        public Object apply(Object operand) {
            if (operand instanceof Number number) {
                return number.doubleValue() - 1;
            }

            throw new UnsupportedOperationException("Unsupported operand for DECREMENT: " + operand);
        }
    }

}
