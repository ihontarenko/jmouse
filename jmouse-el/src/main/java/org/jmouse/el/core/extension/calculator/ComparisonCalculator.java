package org.jmouse.el.core.extension.calculator;

import org.jmouse.el.core.extension.Calculator;
import org.jmouse.util.AnyComparator;

import java.util.function.BiPredicate;

/**
 * Performs comparison operations such as {@code >, <, >=, <=, ==, !=}.
 * This calculator ensures type safety and null handling using {@link org.jmouse.util.AnyComparator}.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * boolean result = ComparisonCalculator.GT.calculate(5, 3); // true
 * boolean result = ComparisonCalculator.EQUAL.calculate("test", "test"); // true
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public enum ComparisonCalculator implements Calculator<Boolean> {

    /**
     * Greater than ({@code >}).
     * Returns {@code true} if {@code left > right}.
     */
    GT(new GreaterThanOperation()),

    /**
     * Less than ({@code <}).
     * Returns {@code true} if {@code left < right}.
     */
    LT(new LessThanOperation()),

    /**
     * Greater than or equal ({@code >=}).
     * Returns {@code true} if {@code left >= right}.
     */
    GTE(new GreaterThanOrEqualOperation()),

    /**
     * Less than or equal ({@code <=}).
     * Returns {@code true} if {@code left <= right}.
     */
    LTE(new LessThanOrEqualOperation()),

    /**
     * Equal ({@code ==}).
     * Returns {@code true} if {@code left.equals(right)}.
     */
    EQUAL(new EqualOperation()),

    /**
     * Not equal ({@code !=}).
     * Returns {@code true} if {@code !left.equals(right)}.
     */
    NOT_EQUAL(new NotEqualOperation());

    private final BiPredicate<Object, Object> operation;

    /**
     * Constructs a new {@link ComparisonCalculator}.
     *
     * @param operation The comparison operation implementation.
     */
    ComparisonCalculator(BiPredicate<Object, Object> operation) {
        this.operation = operation;
    }

    @Override
    public Boolean calculate(Object... operands) {
        if (operands.length != 2) {
            throw new IllegalArgumentException("Comparison operators require exactly 2 operands.");
        }

        return operation.test(operands[0], operands[1]);
    }

    /**
     * Greater than operation ({@code >}).
     */
    public static class GreaterThanOperation implements BiPredicate<Object, Object> {
        @Override
        public boolean test(Object left, Object right) {
            return AnyComparator.compare(left, right) > 0;
        }
    }

    /**
     * Less than operation ({@code <}).
     */
    public static class LessThanOperation implements BiPredicate<Object, Object> {
        @Override
        public boolean test(Object left, Object right) {
            return AnyComparator.compare(left, right) < 0;
        }
    }

    /**
     * Greater than or equal operation ({@code >=}).
     */
    public static class GreaterThanOrEqualOperation implements BiPredicate<Object, Object> {
        @Override
        public boolean test(Object left, Object right) {
            return AnyComparator.compare(left, right) >= 0;
        }
    }

    /**
     * Less than or equal operation ({@code <=}).
     */
    public static class LessThanOrEqualOperation implements BiPredicate<Object, Object> {
        @Override
        public boolean test(Object left, Object right) {
            return AnyComparator.compare(left, right) <= 0;
        }
    }

    /**
     * Equal operation ({@code ==}).
     */
    public static class EqualOperation implements BiPredicate<Object, Object> {
        @Override
        public boolean test(Object left, Object right) {
            return left.equals(right);
        }
    }

    /**
     * Not equal operation ({@code !=}).
     */
    public static class NotEqualOperation implements BiPredicate<Object, Object> {
        @Override
        public boolean test(Object left, Object right) {
            return !left.equals(right);
        }
    }

}

