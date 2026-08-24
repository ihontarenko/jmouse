package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.core.AnyComparator;

import java.util.function.BiPredicate;

/**
 * Performs comparison operations such as {@code >, <, >=, <=, ==, !=}.
 * This calculator ensures type safety and null handling using {@link AnyComparator}.
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
     * Returns {@code true} if the two are not equal — null-safe, and the exact negation of {@code ==}.
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
            if (left == null && right == null) {
                return true;
            } else if (left == null) {
                return false;
            }
            return left.equals(right);
        }
    }

    /**
     * Not equal operation ({@code !=}) — <strong>the exact negation of {@link EqualOperation}</strong>.
     *
     * <p>⚠️ <strong>It delegates rather than repeating the logic, and that is the whole fix.</strong>
     * This used to be {@code !left.equals(right)} with no null handling at all, while {@code ==} beside
     * it handled null carefully. So {@code a == b} answered and {@code a != b} <em>threw</em> for the
     * same pair — an asymmetry nobody would guess and nothing announced.
     *
     * <p>Where that lands is worse than a stack trace. In an authorization rule an operator that throws
     * makes the condition unanswerable, and an unanswerable <em>deny</em> is applied fail-closed — so
     * {@code purpose != 'ASSET'} refused every call that published no purpose. One missing null check
     * took a whole subject area off the air (Innoventa INVT-0126), and it stayed invisible until
     * somebody made the first workspace of that kind.
     *
     * <p>Negating the sibling is what stops the two ever disagreeing again. Repeating the null handling
     * here would be the same bug waiting for the next edit.
     */
    public static class NotEqualOperation implements BiPredicate<Object, Object> {

        private static final EqualOperation EQUALITY = new EqualOperation();

        @Override
        public boolean test(Object left, Object right) {
            return !EQUALITY.test(left, right);
        }
    }

}

