package org.jmouse.validator.constraint.constraint;

import org.jmouse.util.Numbers;
import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.math.BigDecimal;

/**
 * Numeric constraint supporting minimum, maximum, and range validation. 📏
 *
 * <p>
 * {@code MinMaxConstraint} validates numeric values using {@link BigDecimal}
 * comparison semantics. The behavior is defined by {@link Mode}.
 * </p>
 *
 * <h3>Modes</h3>
 * <ul>
 *     <li>{@link Mode#MIN} — value must be {@code >= min}</li>
 *     <li>{@link Mode#MAX} — value must be {@code <= max}</li>
 *     <li>{@link Mode#RANGE} — value must satisfy both boundaries</li>
 * </ul>
 *
 * <h3>Behavior</h3>
 * <ul>
 *     <li>{@code null} values are considered valid (use a separate {@code required} constraint if needed).</li>
 *     <li>Non-numeric values fail validation.</li>
 *     <li>Comparison uses {@link BigDecimal#compareTo(BigDecimal)}.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * MinMaxConstraint constraint = new MinMaxConstraint();
 * constraint.setMode(MinMaxConstraint.Mode.RANGE);
 * constraint.setMin(BigDecimal.valueOf(18));
 * constraint.setMax(BigDecimal.valueOf(65));
 *
 * boolean valid = constraint.execute(25);   // true
 * boolean invalid = constraint.execute(70); // false
 * }</pre>
 */
public final class MinMaxConstraint implements Constraint {

    /**
     * Validation mode.
     */
    public enum Mode {
        /**
         * Only minimum boundary is enforced.
         */
        MIN,

        /**
         * Only maximum boundary is enforced.
         */
        MAX,

        /**
         * Both minimum and maximum boundaries are enforced.
         */
        RANGE
    }

    private Mode       mode;
    private BigDecimal min;
    private BigDecimal max;
    private String     message;

    /**
     * Returns a stable constraint code.
     *
     * <p>
     * The mode is included in the suffix, e.g.:
     * {@code min_max.MIN}, {@code min_max.MAX}, {@code min_max.RANGE}.
     * </p>
     */
    @Override
    public String code() {
        return "min_max." + getMode();
    }

    /**
     * Returns the configured message override (may be {@code null}).
     */
    @Override
    public String message() {
        return getMessage();
    }

    /**
     * Returns message arguments used during interpolation.
     *
     * @return constraint arguments (min, max)
     */
    @Override
    public Object[] arguments() {
        return new Object[]{min, max};
    }

    /**
     * Returns the shared executor instance.
     */
    @Override
    public ConstraintExecutor<MinMaxConstraint> executor() {
        return Executor.INSTANCE;
    }

    /**
     * Internal executor implementation.
     */
    private static final class Executor
            implements ConstraintExecutor<MinMaxConstraint> {

        private static final Executor INSTANCE = new Executor();

        @Override
        public boolean test(Object value, MinMaxConstraint constraint) {

            // null values are valid by design
            if (value == null) {
                return true;
            }

            BigDecimal decimal = Numbers.toDecimal(value);

            // Non-numeric values fail
            if (decimal == null) {
                return false;
            }

            switch (constraint.mode) {
                case MIN -> {
                    return constraint.min == null
                            || decimal.compareTo(constraint.min) >= 0;
                }
                case MAX -> {
                    return constraint.max == null
                            || decimal.compareTo(constraint.max) <= 0;
                }
                case RANGE -> {
                    if (constraint.min != null
                            && decimal.compareTo(constraint.min) < 0) {
                        return false;
                    }
                    return constraint.max == null
                            || decimal.compareTo(constraint.max) <= 0;
                }
            }

            return false;
        }
    }

    /**
     * @return minimum boundary (nullable)
     */
    public BigDecimal getMin() {
        return min;
    }

    /**
     * Sets minimum boundary.
     */
    public void setMin(BigDecimal min) {
        this.min = min;
    }

    /**
     * @return maximum boundary (nullable)
     */
    public BigDecimal getMax() {
        return max;
    }

    /**
     * Sets maximum boundary.
     */
    public void setMax(BigDecimal max) {
        this.max = max;
    }

    /**
     * @return validation mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Sets validation mode.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * @return custom validation message (never {@code null})
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets custom validation message.
     *
     * <p>
     * Internally normalizes {@code null} to empty string.
     * </p>
     */
    public void setMessage(String message) {
        this.message = Strings.emptyIfNull(message);
    }
}